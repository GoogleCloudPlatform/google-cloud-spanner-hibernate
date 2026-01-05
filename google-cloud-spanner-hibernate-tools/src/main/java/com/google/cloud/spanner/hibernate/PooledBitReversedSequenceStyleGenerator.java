/*
 * Copyright 2019-2023 Google LLC
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

import static org.hibernate.id.enhanced.SequenceStyleGenerator.SEQUENCE_PARAM;
import static org.hibernate.internal.util.config.ConfigurationHelper.extractPropertyValue;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Range;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.DatabaseStructure;
import org.hibernate.id.enhanced.NoopOptimizer;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.enhanced.SequenceStructure;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * @deprecated use {@link
 *     com.google.cloud.spanner.hibernate.annotations.PooledBitReversedSequenceGenerator}
 *     <p>Pooled ID generator that uses a bit-reversed sequence to generate values. These values are
 *     safe to use as the primary key of a table in Cloud Spanner. This is the recommended strategy
 *     for auto-generated numeric primary keys in Cloud Spanner.
 *     <p>Using a bit-reversed sequence for ID generation is recommended above sequences that return
 *     a monotonically increasing value for Cloud Spanner. This generator also supports both an
 *     increment size larger than 1 and an initial value larger than 1. The increment value can not
 *     exceed 200 for GoogleSQL-dialect databases and 60 for PostgreSQL-dialect databases.
 *     <p>Use the {@link #EXCLUDE_RANGE_PARAM} to exclude a range of values that should be skipped
 *     by the generator if your entity table already contains data. The excluded values should be
 *     given as closed range. E.g. "[1,1000]" to skip all values between 1 and 1000 (inclusive).
 *     <p>It is recommended to use a separate sequence for each entity. Set the sequence name to use
 *     for a generator with the SequenceStyleGenerator.SEQUENCE_PARAM parameter (see example below).
 *     <p>Example usage:
 *     <pre>{@code
 * @Id
 * @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customerId")
 * @GenericGenerator(
 *       name = "customerId",
 *       strategy = "com.google.cloud.spanner.hibernate.PooledBitReversedSequenceStyleGenerator",
 *       parameters = {
 *           @Parameter(name = SequenceStyleGenerator.SEQUENCE_PARAM, value = "customerId"),
 *           @Parameter(name = SequenceStyleGenerator.INCREMENT_PARAM, value = "200"),
 *           @Parameter(name = SequenceStyleGenerator.INITIAL_PARAM, value = "50000"),
 *           @Parameter(name = PooledBitReversedSequenceStyleGenerator.EXCLUDE_RANGE_PARAM,
 *                      value = "[1,1000]"),
 *       })
 * @Column(nullable = false)
 * private Long customerId;
 * }</pre>
 */
@Deprecated(forRemoval = true)
public class PooledBitReversedSequenceStyleGenerator
    implements BulkInsertionCapableIdentifierGenerator, PersistentIdentifierGenerator {

  /** The default increment (fetch) size for an {@link PooledBitReversedSequenceStyleGenerator}. */
  public static final int DEFAULT_INCREMENT_SIZE = 50;

  /**
   * Configuration property for defining a range that should be excluded by a bit-reversed sequence
   * generator.
   */
  public static final String EXCLUDE_RANGE_PARAM = "exclude_range";

  /** Legacy parameter name. */
  private static final String EXCLUDE_RANGES_PARAM = "exclude_ranges";

  /**
   * The maximum allowed increment size is 1000 for PostgreSQL-dialect databases. This limitation
   * will be lifted in the future.
   */
  private static final int POSTGRES_MAX_INCREMENT_SIZE = 1000;

  private static final Iterator<Long> EMPTY_ITERATOR = Collections.emptyIterator();
  private final Lock lock = new ReentrantLock();
  private final Optimizer optimizer = new NoopOptimizer(Long.class, 1);

  private Dialect dialect;
  private QualifiedSequenceName sequenceName;
  private String select;
  private int fetchSize;
  private Iterator<Long> identifiers = EMPTY_ITERATOR;
  private DatabaseStructure databaseStructure;

  private static QualifiedSequenceName determineSequenceName(
      JdbcEnvironment jdbcEnvironment, Properties params) {
    String sequenceName = params.getProperty(SEQUENCE_PARAM);
    if (sequenceName == null) {
      throw new MappingException("no sequence name specified");
    }
    if (sequenceName.contains(".")) {
      QualifiedName qualifiedName = QualifiedNameParser.INSTANCE.parse(sequenceName);
      return new QualifiedSequenceName(
          qualifiedName.getCatalogName(),
          qualifiedName.getSchemaName(),
          qualifiedName.getObjectName());
    } else {
      final Identifier catalog =
          jdbcEnvironment
              .getIdentifierHelper()
              .toIdentifier(ConfigurationHelper.getString(CATALOG, params));
      final Identifier schema =
          jdbcEnvironment
              .getIdentifierHelper()
              .toIdentifier(ConfigurationHelper.getString(SCHEMA, params));
      return new QualifiedSequenceName(
          catalog, schema, jdbcEnvironment.getIdentifierHelper().toIdentifier(sequenceName));
    }
  }

  private static String buildSkipRangeOptions(List<Range<Long>> excludeRanges) {
    return String.format(
        "skip_range_min=%d, skip_range_max=%d",
        getMinSkipRange(excludeRanges), getMaxSkipRange(excludeRanges));
  }

  private static long getMinSkipRange(List<Range<Long>> excludeRanges) {
    return excludeRanges.stream().map(Range::lowerEndpoint).min(Long::compare).orElse(0L);
  }

  private static long getMaxSkipRange(List<Range<Long>> excludeRanges) {
    return excludeRanges.stream()
        .map(Range::upperEndpoint)
        .max(Long::compare)
        .orElse(Long.MAX_VALUE);
  }

  private static int determineInitialValue(Properties params) {
    int initialValue =
        ConfigurationHelper.getInt(
            SequenceStyleGenerator.INITIAL_PARAM,
            params,
            SequenceStyleGenerator.DEFAULT_INITIAL_VALUE);
    if (initialValue <= 0) {
      throw new MappingException("initial value must be positive");
    }
    return initialValue;
  }

  @VisibleForTesting
  static List<Range<Long>> parseExcludedRanges(String sequenceName, Properties params) {
    // Accept both 'excluded_range' and 'excluded_ranges' params to accommodate anyone moving from
    // the original BitReversedSequenceStyleGenerator to PooledBitReversedSequenceStyleGenerator.
    String[] excludedRangesArray =
        extractPropertyValue(EXCLUDE_RANGES_PARAM, params) != null
            ? StringHelper.split(" ", extractPropertyValue(EXCLUDE_RANGES_PARAM, params))
            : ArrayHelper.EMPTY_STRING_ARRAY;
    String[] excludedRangeArray =
        extractPropertyValue(EXCLUDE_RANGE_PARAM, params) != null
            ? StringHelper.split(" ", extractPropertyValue(EXCLUDE_RANGE_PARAM, params))
            : ArrayHelper.EMPTY_STRING_ARRAY;
    if (excludedRangesArray == null && excludedRangeArray == null) {
      return ImmutableList.of();
    }
    if (excludedRangesArray != null && excludedRangeArray != null) {
      // Combine the two arrays.
      String[] newArray = new String[excludedRangeArray.length + excludedRangesArray.length];
      System.arraycopy(excludedRangeArray, 0, newArray, 0, excludedRangeArray.length);
      System.arraycopy(
          excludedRangesArray, 0, newArray, excludedRangeArray.length, excludedRangesArray.length);
      excludedRangesArray = newArray;
    } else if (excludedRangeArray != null) {
      excludedRangesArray = excludedRangeArray;
    }
    Builder<Range<Long>> builder = ImmutableList.builder();
    for (String rangeString : excludedRangesArray) {
      rangeString = rangeString.trim();
      String invalidRangeMessage =
          String.format(
              "Invalid range found for the [%s] sequence: %%s\n"
                  + "Excluded ranges must be given as a space-separated sequence of ranges between "
                  + "square brackets, e.g. '[1,1000] [2001,3000]'. "
                  + "Found '%s'",
              sequenceName, rangeString);
      if (!(rangeString.startsWith("[") && rangeString.endsWith("]"))) {
        throw new MappingException(
            String.format(invalidRangeMessage, "Range is not enclosed between '[' and ']'"));
      }
      rangeString = rangeString.substring(1, rangeString.length() - 1);
      String[] values = rangeString.split(",");
      if (values.length != 2) {
        throw new MappingException(
            String.format(invalidRangeMessage, "Range does not contain exactly two elements"));
      }
      long from;
      long to;
      try {
        from = Long.parseLong(values[0]);
        to = Long.parseLong(values[1]);
        builder.add(Range.closed(from, to));
      } catch (IllegalArgumentException e) {
        throw new MappingException(String.format(invalidRangeMessage, e.getMessage()), e);
      }
    }
    return builder.build();
  }

  @Override
  public Optimizer getOptimizer() {
    return optimizer;
  }

  @Override
  public void configure(Type type, Properties params, ServiceRegistry serviceRegistry)
      throws MappingException {
    JdbcEnvironment jdbcEnvironment = serviceRegistry.getService(JdbcEnvironment.class);
    this.dialect = jdbcEnvironment.getDialect();
    this.sequenceName = determineSequenceName(jdbcEnvironment, params);
    this.fetchSize = determineFetchSize(params);
    int initialValue = determineInitialValue(params);
    this.select = buildSelect(sequenceName, fetchSize);
    List<Range<Long>> excludeRanges =
        parseExcludedRanges(sequenceName.getObjectName().getText(), params);
    this.databaseStructure =
        buildDatabaseStructure(
            determineContributor(params),
            type,
            sequenceName,
            initialValue,
            excludeRanges,
            jdbcEnvironment);
  }

  private String determineContributor(Properties params) {
    final String contributor = params.getProperty(IdentifierGenerator.CONTRIBUTOR_NAME);
    return contributor == null ? "orm" : contributor;
  }

  private int determineFetchSize(Properties params) {
    int fetchSize;
    if (ConfigurationHelper.getInteger("fetch_size", params) != null) {
      fetchSize = ConfigurationHelper.getInt("fetch_size", params, DEFAULT_INCREMENT_SIZE);
    } else {
      fetchSize =
          ConfigurationHelper.getInt(
              SequenceStyleGenerator.INCREMENT_PARAM, params, DEFAULT_INCREMENT_SIZE);
    }
    if (fetchSize <= 0) {
      throw new MappingException("increment size must be positive");
    }
    if (fetchSize > getMaxIncrementSize()) {
      throw new MappingException("increment size must be <= " + getMaxIncrementSize());
    }
    return fetchSize;
  }

  private int getMaxIncrementSize() {
    return isPostgres() ? POSTGRES_MAX_INCREMENT_SIZE : Integer.MAX_VALUE;
  }

  private SequenceStructure buildDatabaseStructure(
      String contributor,
      Type type,
      QualifiedSequenceName sequenceName,
      int initialValue,
      List<Range<Long>> excludeRanges,
      JdbcEnvironment jdbcEnvironment) {
    if (isPostgres()) {
      return new BitReversedSequenceStructure(
          jdbcEnvironment,
          contributor,
          sequenceName,
          initialValue,
          1,
          excludeRanges,
          type.getReturnedClass());
    }
    return new SequenceStructure(
        contributor,
        sequenceName,
        initialValue,
        1,
        excludeRanges.isEmpty() ? "" : buildSkipRangeOptions(excludeRanges),
        type.getReturnedClass());
  }

  private String buildSelect(QualifiedSequenceName sequenceName, int fetchSize) {
    String hints =
        "/* spanner.force_read_write_transaction=true */ "
            + "/* spanner.ignore_during_internal_retry=true */ ";

    if (isPostgres()) {
      return String.format(
          "%s select %s",
          hints,
          IntStream.range(0, fetchSize)
              .mapToObj(
                  ignore -> "nextval('" + sequenceName.getSequenceName().getText() + "') as n")
              .collect(Collectors.joining(", ")));
    }
    return String.format(
        "%s select get_next_sequence_value(sequence %s) AS n "
            + "from unnest(generate_array(1, %d))",
        hints, sequenceName.getSequenceName().getText(), fetchSize);
  }

  @VisibleForTesting
  String getSelect() {
    return this.select;
  }

  private boolean isPostgres() {
    return this.dialect.openQuote() == '"';
  }

  @Override
  public boolean supportsBulkInsertionIdentifierGeneration() {
    return true;
  }

  @Override
  public String determineBulkInsertionIdentifierGenerationSelectFragment(
      SqlStringGenerationContext context) {
    return context
        .getDialect()
        .getSequenceSupport()
        .getSelectSequenceNextValString(getSequenceName());
  }

  @Override
  public Serializable generate(SharedSessionContractImplementor session, Object object)
      throws HibernateException {
    this.lock.lock();
    try {
      while (!this.identifiers.hasNext()) {
        this.identifiers = fetchIdentifiers(session);
      }
      return this.identifiers.next();
    } finally {
      this.lock.unlock();
    }
  }

  private String getSequenceName() {
    return this.databaseStructure.getPhysicalName().getObjectName().getCanonicalName();
  }

  @Override
  public void registerExportables(Database database) {
    Namespace namespace =
        database.locateNamespace(sequenceName.getCatalogName(), sequenceName.getSchemaName());
    Sequence sequence = namespace.locateSequence(sequenceName.getSequenceName());
    if (sequence == null) {
      this.databaseStructure.registerExportables(database);
    }
  }

  private Iterator<Long> fetchIdentifiers(SharedSessionContractImplementor session)
      throws HibernateException {
    // Prefix all 'set ...' statements with 'spanner.' if the dialect is PostgreSQL.
    // The safest way to determine that is by looking at the quote character for identifiers.
    String extensionPrefix = dialect.openQuote() == '"' ? "spanner." : "";
    Connection connection = null;
    Boolean retryAbortsInternally = null;
    try {
      // Use a separate connection to get new sequence values. This ensures that it also uses a
      // separate read/write transaction, which again means that it will not interfere with any
      // retries of the actual business transaction.
      connection = session.getJdbcConnectionAccess().obtainConnection();
      connection.setAutoCommit(false);
      try (Statement statement = connection.createStatement()) {
        // TODO: Use 'set local spanner.retry_aborts_internally=false' when that has been
        //       implemented.
        retryAbortsInternally = isRetryAbortsInternally(statement);
        connection.commit();
        statement.execute(String.format("set %sretry_aborts_internally=false", extensionPrefix));
        List<Long> identifiers = new ArrayList<>(this.fetchSize);
        session.getJdbcServices().getSqlStatementLogger().logStatement(this.select);
        try (ResultSet resultSet = statement.executeQuery(this.select)) {
          while (resultSet.next()) {
            for (int col = 1; col <= resultSet.getMetaData().getColumnCount(); col++) {
              identifiers.add(resultSet.getLong(col));
            }
          }
        }
        // Do a rollback instead of a commit here because:
        // 1. We have only accessed a bit-reversed sequence during the transaction.
        // 2. Committing or rolling back the transaction does not make any difference for the
        //    sequence. Its state has been updated in both cases.
        // 3. Committing the transaction on the emulator would cause it to be aborted, as the
        //    emulator only supports one transaction at any time. Rolling back is however allowed.
        connection.rollback();
        return identifiers.iterator();
      }
    } catch (SQLException sqlException) {
      if (connection != null) {
        ignoreSqlException(connection::rollback);
      }
      if (isAbortedError(sqlException)) {
        // Return an empty iterator to force a retry.
        return EMPTY_ITERATOR;
      }
      throw session
          .getJdbcServices()
          .getSqlExceptionHelper()
          .convert(sqlException, "could not get next sequence values", this.select);
    } finally {
      if (connection != null) {
        Connection finalConnection = connection;
        if (retryAbortsInternally != null) {
          Boolean finalRetryAbortsInternally = retryAbortsInternally;
          ignoreSqlException(
              () ->
                  finalConnection
                      .createStatement()
                      .execute(
                          String.format(
                              "set %sretry_aborts_internally=%s",
                              extensionPrefix, finalRetryAbortsInternally)));
          ignoreSqlException(connection::commit);
        }
        ignoreSqlException(
            () -> session.getJdbcConnectionAccess().releaseConnection(finalConnection));
      }
    }
  }

  private Boolean isRetryAbortsInternally(Statement statement) {
    String prefix = dialect.openQuote() == '"' ? "spanner." : "variable ";
    try (ResultSet resultSet =
        statement.executeQuery(String.format("show %sretry_aborts_internally", prefix))) {
      if (resultSet.next()) {
        return resultSet.getBoolean(1);
      }
      return null;
    } catch (Throwable ignore) {
      return null;
    }
  }

  private boolean isAbortedError(SQLException exception) {
    // '40001' == serialization_failure
    if ("40001".equals(exception.getSQLState())) {
      return true;
    }
    // 10 == Aborted
    return exception.getErrorCode() == 10;
  }

  private void ignoreSqlException(SqlRunnable runnable) {
    try {
      runnable.run();
    } catch (SQLException ignore) {
      // ignore any SQLException
    }
  }

  @Override
  public String toString() {
    return getSequenceName();
  }

  private interface SqlRunnable {

    void run() throws SQLException;
  }
}
