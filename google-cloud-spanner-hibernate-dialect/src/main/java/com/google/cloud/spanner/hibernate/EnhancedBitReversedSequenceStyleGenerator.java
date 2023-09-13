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

import static com.google.cloud.spanner.hibernate.BitReversedSequenceStyleGenerator.EXCLUDE_RANGES_PARAM;
import static org.hibernate.id.enhanced.SequenceStyleGenerator.SEQUENCE_PARAM;

import com.google.cloud.spanner.jdbc.CloudSpannerJdbcConnection;
import com.google.cloud.spanner.jdbc.JdbcSqlException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Range;
import com.google.rpc.Code;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.ArrayUtils;
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
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.DatabaseStructure;
import org.hibernate.id.enhanced.SequenceStructure;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * ID generator that uses a bit-reversed sequence to generate values. These values are safe to use
 * as the primary key of a table in Cloud Spanner. This is the recommended strategy for
 * auto-generated numeric primary keys in Cloud Spanner.
 *
 * <p>Using a bit-reversed sequence for ID generation is recommended above sequences that return a
 * monotonically increasing value for Cloud Spanner. This generator also supports both an increment
 * size larger than 1 and an initial value larger than 1. The increment value can not exceed 200.
 *
 * <p>Use the {@link #EXCLUDE_RANGE_PARAM} to exclude a range of values that should be skipped by
 * the generator if your entity table already contains data. The excluded values should be given as
 * closed range. E.g. "[1,1000]" to skip all values between 1 and 1000 (inclusive).
 *
 * <p>It is recommended to use a separate sequence for each entity. Set the sequence name to use
 * for
 * a generator with the SequenceStyleGenerator.SEQUENCE_PARAM parameter (see example below).
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Id
 * @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customerId")
 * @GenericGenerator(
 *       name = "customerId",
 *       strategy = "com.google.cloud.spanner.hibernate.EnhancedBitReversedSequenceStyleGenerator",
 *       parameters = {
 *           @Parameter(name = SequenceStyleGenerator.SEQUENCE_PARAM, value = "customerId"),
 *           @Parameter(name = SequenceStyleGenerator.INCREMENT_PARAM, value = "200"),
 *           @Parameter(name = SequenceStyleGenerator.INITIAL_PARAM, value = "50000"),
 *           @Parameter(name = EnhancedBitReversedSequenceStyleGenerator.EXCLUDE_RANGE_PARAM,
 *                      value = "[1,1000]"),
 *       })
 * @Column(nullable = false)
 * private Long customerId;
 * }</pre>
 */
public class EnhancedBitReversedSequenceStyleGenerator implements
    BulkInsertionCapableIdentifierGenerator, PersistentIdentifierGenerator {

  /**
   * The default increment (fetch) size for an {@link EnhancedBitReversedSequenceStyleGenerator}.
   */
  public static final int DEFAULT_INCREMENT_SIZE = 100;
  /**
   * Configuration property for defining a range that should be excluded by a bit-reversed sequence
   * generator.
   */
  public static final String EXCLUDE_RANGE_PARAM = "exclude_range";
  /**
   * The maximum allowed increment size is 200.
   */
  private static final int MAX_INCREMENT_SIZE = 200;
  private static final Iterator<Long> EMPTY_ITERATOR = ImmutableList.<Long>of().iterator();
  private final Lock lock = new ReentrantLock();

  private QualifiedSequenceName sequenceName;
  private String select;
  private int fetchSize;
  private Iterator<Long> identifiers = EMPTY_ITERATOR;
  private DatabaseStructure databaseStructure;

  private static String buildSelect(Dialect dialect, QualifiedSequenceName sequenceName,
      int fetchSize) {
    return "WITH t AS (\n" + IntStream.range(0, fetchSize).mapToObj(
            ignore -> "\t" + dialect.getSequenceNextValString(sequenceName.render()) + " AS n")
        .collect(Collectors.joining("\n\tUNION ALL\n")) + "\n)\n" + "SELECT n FROM t";
  }

  private static SequenceStructure buildDatabaseStructure(Type type,
      QualifiedSequenceName sequenceName, int initialValue, List<Range<Long>> excludeRanges,
      JdbcEnvironment jdbcEnvironment) {
    if (!excludeRanges.isEmpty()) {
      // Put the excluded range in the catalog name. We have no other way of getting that
      // information into the sequence. The SpannerSequenceExporter then extracts this information
      // and removes the bogus catalog name.
      sequenceName = new QualifiedSequenceName(
          Identifier.toIdentifier(buildSkipRangeOptions(excludeRanges)),
          sequenceName.getSchemaName(), sequenceName.getObjectName());
    }
    return new SequenceStructure(jdbcEnvironment, sequenceName, initialValue, 1,
        type.getReturnedClass());
  }

  private static String buildSkipRangeOptions(List<Range<Long>> excludeRanges) {
    return String.format("skip_range_min=%d, skip_range_max=%d", getMinSkipRange(excludeRanges),
        getMaxSkipRange(excludeRanges));
  }

  private static long getMinSkipRange(List<Range<Long>> excludeRanges) {
    return excludeRanges.stream().map(Range::lowerEndpoint).min(Long::compare).orElse(0L);
  }

  private static long getMaxSkipRange(List<Range<Long>> excludeRanges) {
    return excludeRanges.stream().map(Range::upperEndpoint).max(Long::compare)
        .orElse(Long.MAX_VALUE);
  }

  private static QualifiedSequenceName determineSequenceName(JdbcEnvironment jdbcEnvironment,
      Properties params) {
    String sequenceName = params.getProperty(SEQUENCE_PARAM);
    if (sequenceName == null) {
      throw new MappingException("no sequence name specified");
    }
    if (sequenceName.contains(".")) {
      QualifiedName qualifiedName = QualifiedNameParser.INSTANCE.parse(sequenceName);
      return new QualifiedSequenceName(qualifiedName.getCatalogName(),
          qualifiedName.getSchemaName(), qualifiedName.getObjectName());
    } else {
      final Identifier catalog = jdbcEnvironment.getIdentifierHelper()
          .toIdentifier(ConfigurationHelper.getString(CATALOG, params));
      final Identifier schema = jdbcEnvironment.getIdentifierHelper()
          .toIdentifier(ConfigurationHelper.getString(SCHEMA, params));
      return new QualifiedSequenceName(catalog, schema,
          jdbcEnvironment.getIdentifierHelper().toIdentifier(sequenceName));
    }
  }

  private static int determineFetchSize(Properties params) {
    int fetchSize;
    if (ConfigurationHelper.getInteger("fetch_size", params) != null) {
      fetchSize = ConfigurationHelper.getInt("fetch_size", params, DEFAULT_INCREMENT_SIZE);
    } else {
      fetchSize = ConfigurationHelper.getInt(SequenceStyleGenerator.INCREMENT_PARAM, params,
          DEFAULT_INCREMENT_SIZE);
    }
    if (fetchSize <= 0) {
      throw new MappingException("increment size must be positive");
    }
    if (fetchSize > MAX_INCREMENT_SIZE) {
      throw new MappingException("increment size must be <= " + MAX_INCREMENT_SIZE);
    }
    return fetchSize;
  }

  private static int determineInitialValue(Properties params) {
    int initialValue = ConfigurationHelper.getInt(SequenceStyleGenerator.INITIAL_PARAM, params,
        SequenceStyleGenerator.DEFAULT_INITIAL_VALUE);
    if (initialValue <= 0) {
      throw new MappingException("initial value must be positive");
    }
    return initialValue;
  }

  @VisibleForTesting
  static List<Range<Long>> parseExcludedRanges(String sequenceName, Properties params) {
    // Accept both 'excluded_range' and 'excluded_ranges' params to accommodate anyone moving from
    // the original BitReversedSequenceStyleGenerator to EnhancedBitReversedSequenceStyleGenerator.
    String[] excludedRangesArray =
        ConfigurationHelper.toStringArray(EXCLUDE_RANGES_PARAM, " ", params);
    String[] excludedRangeArray =
        ConfigurationHelper.toStringArray(EXCLUDE_RANGE_PARAM, " ", params);
    if (excludedRangesArray == null && excludedRangeArray == null) {
      return ImmutableList.of();
    }
    if (excludedRangesArray != null && excludedRangeArray != null) {
      excludedRangesArray = ArrayUtils.addAll(excludedRangesArray, excludedRangeArray);
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
  public void configure(Type type, Properties params, ServiceRegistry serviceRegistry)
      throws MappingException {
    JdbcEnvironment jdbcEnvironment = serviceRegistry.getService(JdbcEnvironment.class);
    this.sequenceName = determineSequenceName(jdbcEnvironment, params);
    this.fetchSize = determineFetchSize(params);
    int initialValue = determineInitialValue(params);
    this.select = buildSelect(jdbcEnvironment.getDialect(), sequenceName, fetchSize);
    List<Range<Long>> excludeRanges = parseExcludedRanges(sequenceName.getObjectName().getText(),
        params);
    this.databaseStructure = buildDatabaseStructure(type, sequenceName, initialValue, excludeRanges,
        jdbcEnvironment);
  }

  @Override
  public boolean supportsBulkInsertionIdentifierGeneration() {
    return true;
  }

  @Override
  public String determineBulkInsertionIdentifierGenerationSelectFragment(
      SqlStringGenerationContext context) {
    return context.getDialect().getSelectSequenceNextValString(getSequenceName());
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
    Namespace namespace = database.locateNamespace(sequenceName.getCatalogName(),
        sequenceName.getSchemaName());
    Sequence sequence = namespace.locateSequence(sequenceName.getSequenceName());
    if (sequence == null) {
      this.databaseStructure.registerExportables(database);
    }
  }

  private Iterator<Long> fetchIdentifiers(SharedSessionContractImplementor session)
      throws HibernateException {
    Connection connection = null;
    Boolean retryAbortsInternally = null;
    try {
      // Use a separate connection to get new sequence values. This ensures that it also uses a
      // separate read/write transaction, which again means that it will not interfere with any
      // retries of the actual business transaction.
      connection = session.getJdbcConnectionAccess().obtainConnection();
      if (connection.isWrapperFor(CloudSpannerJdbcConnection.class)) {
        // Do not try to retry any aborted errors, as a sequence will return new values in all
        // cases.
        CloudSpannerJdbcConnection cloudSpannerJdbcConnection = connection.unwrap(
            CloudSpannerJdbcConnection.class);
        retryAbortsInternally = cloudSpannerJdbcConnection.isRetryAbortsInternally();
        cloudSpannerJdbcConnection.setRetryAbortsInternally(false);
      }
      try (Statement statement = connection.createStatement()) {
        statement.execute("begin");
        statement.execute("set transaction read write");
        List<Long> identifiers = new ArrayList<>(this.fetchSize);
        try (ResultSet resultSet = statement.executeQuery(this.select)) {
          while (resultSet.next()) {
            identifiers.add(resultSet.getLong(1));
          }
        }
        connection.createStatement().execute("commit");
        return identifiers.iterator();
      }
    } catch (SQLException sqlException) {
      if (connection != null) {
        Connection finalConnection = connection;
        ignoreSqlException(() -> finalConnection.createStatement().execute("rollback"));
      }
      if (sqlException instanceof JdbcSqlException) {
        JdbcSqlException jdbcSqlException = (JdbcSqlException) sqlException;
        if (jdbcSqlException.getCode() == Code.ABORTED) {
          return EMPTY_ITERATOR;
        }
      }
      throw session.getJdbcServices().getSqlExceptionHelper()
          .convert(sqlException, "could not get next sequence values", this.select);
    } finally {
      if (connection != null) {
        Connection finalConnection = connection;
        if (retryAbortsInternally != null) {
          boolean finalRetryAbortsInternally = retryAbortsInternally;
          ignoreSqlException(() -> finalConnection.unwrap(CloudSpannerJdbcConnection.class)
              .setRetryAbortsInternally(finalRetryAbortsInternally));
        }
        ignoreSqlException(
            () -> session.getJdbcConnectionAccess().releaseConnection(finalConnection));
      }
    }
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
