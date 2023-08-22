/*
 * Copyright 2019-2020 Google LLC
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package com.google.cloud.spanner.hibernate;

import static com.google.cloud.spanner.hibernate.EnhancedBitReversedSequenceStyleGenerator.parseExcludedRanges;

import com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory.JdbcAbortedDueToConcurrentModificationException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.io.Serializable;
import java.util.List;
import java.util.Properties;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.enhanced.DatabaseStructure;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableStructure;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.jboss.logging.Logger;

/**
 * Table backed ID generator that reverses the bits in the returned sequence value.
 * 
 * <p>This generator uses a <strong>table to emulate a sequence</strong>. Cloud Spanner also
 * supports bit-reversed sequences that are stored and managed in the database. These are
 * recommended above this table backed solution.
 *
 * <p>Using a bit-reversed sequence for ID generation is recommended above sequences that return a
 * monotonically increasing value for Cloud Spanner. This generator also supports both an increment
 * size larger than 1 and an initial value larger than 1.
 *
 * <p>Use the BitReversedSequenceStyleGenerator.EXCLUDE_RANGES_PARAM to exclude ranges of values
 * that should be skipped by the generator if your entity table already contains data. The excluded
 * values should be given as space-separated closed ranges. E.g. "[1,1000] [2001,3000]" to skip all
 * values between 1 and 1000 (inclusive) and between 2001 and 3000 (inclusive).
 *
 * <p>It is recommended to use a separate table for each generator to prevent a large number of
 * writes for a single ID generator table. Set the table name to use for a generator with the
 * SequenceStyleGenerator.SEQUENCE_PARAM parameter (see example below).
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Id
 * @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customerId")
 * @GenericGenerator(
 *       name = "customerId",
 *       strategy = "com.google.cloud.spanner.hibernate.BitReversedSequenceStyleGenerator",
 *       parameters = {
 *           @Parameter(name = SequenceStyleGenerator.SEQUENCE_PARAM, value = "customerId"),
 *           @Parameter(name = SequenceStyleGenerator.INCREMENT_PARAM, value = "1000"),
 *           @Parameter(name = SequenceStyleGenerator.INITIAL_PARAM, value = "50000"),
 *           @Parameter(name = BitReversedSequenceStyleGenerator.EXCLUDE_RANGES_PARAM,
 *                      value = "[1,1000] [10000,20000]"),
 *       })
 * @Column(nullable = false)
 * private Long customerId;
 * }</pre>
 *
 * @deprecated Use {@link EnhancedBitReversedSequenceStyleGenerator} instead.
 */
@Deprecated
public class BitReversedSequenceStyleGenerator extends SequenceStyleGenerator {
  private static final Logger log = Logger.getLogger(BitReversedSequenceStyleGenerator.class);

  /** Specific implementation of a backing {@link TableStructure} for bit-reversed sequences. */
  private static class SpannerSequenceTableStructure extends TableStructure {
    private final JdbcEnvironment jdbcEnvironment;
    private final QualifiedName qualifiedName;
    private final Identifier valueColumnNameIdentifier;
    private final int initialValue;

    public SpannerSequenceTableStructure(
        JdbcEnvironment jdbcEnvironment,
        QualifiedName qualifiedTableName,
        Identifier valueColumnNameIdentifier,
        int initialValue,
        int incrementSize,
        Class numberType) {
      super(
          jdbcEnvironment,
          qualifiedTableName,
          valueColumnNameIdentifier,
          initialValue,
          incrementSize,
          numberType);
      this.jdbcEnvironment = jdbcEnvironment;
      this.qualifiedName = qualifiedTableName;
      this.valueColumnNameIdentifier = valueColumnNameIdentifier;
      this.initialValue = initialValue;
    }

    @Override
    public void registerExportables(Database database) {
      super.registerExportables(database);
      // Replace the init command for the table-backed sequence.
      // Hibernate by default generates an 'insert into table_name values (?)' statement.
      // That is not supported by Cloud Spanner, as Cloud Spanner requires the insert statement to
      // include the column name(s) that are being used in the insert statement.
      final Namespace namespace =
          database.locateNamespace(qualifiedName.getCatalogName(), qualifiedName.getSchemaName());
      Table table = namespace.locateTable(qualifiedName.getObjectName());
      if (table != null) {
        Dialect dialect = jdbcEnvironment.getDialect();
        String valueColumnNameText = valueColumnNameIdentifier.render(dialect);
        table.addInitCommand(
            context ->
                new ReplaceInitCommand(
                    "insert into "
                        + context.format(table.getQualifiedTableName())
                        + " ("
                        + valueColumnNameText
                        + ") values ( "
                        + initialValue
                        + " )"));
      }
    }
  }

  /**
   * Acts as a replacement for other {@link InitCommand}.
   *
   * <ol>
   *   <li>If the list contains at least one {@link ReplaceInitCommand} and at least one normal
   *       {@link InitCommand}, then all normal {@link InitCommand}s will be ignored during
   *       execution and only the {@link ReplaceInitCommand}s will be executed.
   *   <li>If the list only contains {@link ReplaceInitCommand}s, nothing will be executed.
   *   <li>If the list only contains normal {@link InitCommand}s, all normal {@link InitCommand}s
   *       will be executed as normal.
   * </ol>
   */
  public static class ReplaceInitCommand extends InitCommand {
    public ReplaceInitCommand(String... initCommands) {
      super(initCommands);
    }
  }

  /**
   * Configuration property for defining ranges that should be excluded by a bit-reversed sequence
   * generator.
   */
  public static final String EXCLUDE_RANGES_PARAM = "exclude_ranges";

  private QualifiedName sequenceName;

  /**
   * A set of ranges that should be excluded from generation by this generator. This can be used to
   * make sure that existing values from a previous system are not re-generated by this generator.
   * The lower and upper bounds of the ranges are always closed.
   */
  private List<Range<Long>> excludeRanges = ImmutableList.of();

  @Override
  protected DatabaseStructure buildTableStructure(
      Type type,
      Properties params,
      JdbcEnvironment jdbcEnvironment,
      QualifiedName sequenceName,
      int initialValue,
      int incrementSize) {
    this.sequenceName = sequenceName;
    final Identifier valueColumnName = determineValueColumnName(params, jdbcEnvironment);
    return new SpannerSequenceTableStructure(
        jdbcEnvironment,
        sequenceName,
        valueColumnName,
        initialValue,
        incrementSize,
        type.getReturnedClass());
  }

  @Override
  public void configure(Type type, Properties params, ServiceRegistry serviceRegistry)
      throws MappingException {
    // Force the use of a table-backed sequence for this generator, as Cloud Spanner does not
    // support regular sequences (only bit-reversed sequences).
    params.put(FORCE_TBL_PARAM, true);
    super.configure(type, params, serviceRegistry);
    configureExcludedRanges(this.sequenceName.getObjectName().getText(), params);
  }

  @VisibleForTesting
  void configureExcludedRanges(String sequenceName, Properties params) throws MappingException {
    this.excludeRanges = parseExcludedRanges(sequenceName, params);
  }

  @VisibleForTesting static final int MAX_ATTEMPTS = 100;

  /**
   * Generates a new ID. This uses the normal sequence strategy, but the returned ID is bit-reversed
   * before it is returned to the application.
   */
  @Override
  public Serializable generate(SharedSessionContractImplementor session, Object object)
      throws HibernateException {
    Serializable id;
    // Loop to skip excluded ranges.
    while (true) {
      int attempts = 0;
      // Loop to retry the transaction that updates the table-backed sequence if it fails due to a
      // concurrent modification error. This can happen if multiple entities are using the same
      // sequence for generated primary keys.
      while (true) {
        try {
          id = generateBaseValue(session, object);
          break;
        } catch (GenericJDBCException exception) {
          JdbcAbortedDueToConcurrentModificationException aborted;
          if (exception.getSQLException()
              instanceof JdbcAbortedDueToConcurrentModificationException) {
            aborted = (JdbcAbortedDueToConcurrentModificationException) exception.getSQLException();
          } else {
            throw exception;
          }
          attempts++;
          if (attempts == MAX_ATTEMPTS) {
            throw exception;
          }
          try {
            sleep(aborted.getCause().getRetryDelayInMillis());
          } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IdentifierGenerationException(
                "Interrupted while trying to generate a new ID", interruptedException);
          }
        }
      }
      if (id instanceof Long) {
        Long reversed = Long.reverse((Long) id);
        if (excludeRanges.stream().noneMatch(range -> range.contains(reversed))) {
          return reversed;
        }
        log.debugf(
            "Skipping reversed id %d (original id %d) as it is in an excluded range", reversed, id);
      } else {
        return id;
      }
    }
  }

  @VisibleForTesting
  protected void sleep(long millis) throws InterruptedException {
    Thread.sleep(millis);
  }

  @VisibleForTesting
  protected Serializable generateBaseValue(
      SharedSessionContractImplementor session, Object object) {
    return super.generate(session, object);
  }
}
