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

import com.google.cloud.spanner.jdbc.JdbcSqlException;
import com.google.common.collect.ImmutableList;
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
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * Sequence generator that uses a bit-reversed sequence and that fetches multiple values from the
 * sequence in a single round-trip. This ensures that the generator is compatible with batching.
 */
public class BatchedBitReversedSequenceStyleGenerator implements
    BulkInsertionCapableIdentifierGenerator, PersistentIdentifierGenerator {

  /**
   * The number of values to fetch at once. The default is {@link #DEFAULT_FETCH_SIZE}.
   */
  public static final String FETCH_SIZE_PARAM = "fetch_size";
  public static final int DEFAULT_FETCH_SIZE = 100;
  /**
   * The start value of the sequence. The first returned value of the sequence will be the
   * bit-reversed value of this initial value.
   */
  public static final String INITIAL_VALUE_PARAM = "initial_value";
  public static final int DEFAULT_INITIAL_VALUE = 1;
  private static final Iterator<Long> EMPTY_ITERATOR = ImmutableList.<Long>of().iterator();
  private final Lock lock = new ReentrantLock();

  private QualifiedSequenceName sequenceName;
  private String select;
  private int fetchSize;
  private Iterator<Long> identifiers = EMPTY_ITERATOR;
  private DatabaseStructure databaseStructure;

  private static String buildSelect(Dialect dialect, QualifiedSequenceName sequenceName,
      int fetchSize) {
    return "WITH t AS (\n" + IntStream.range(0, fetchSize)
        .mapToObj(
            ignore -> "\t" + dialect.getSequenceNextValString(sequenceName.render()) + " AS n")
        .collect(Collectors.joining("\n\tUNION ALL\n")) + "\n)\n" + "SELECT n FROM t";
  }

  private static SequenceStructure buildDatabaseStructure(Type type,
      QualifiedSequenceName sequenceName, int initialValue, JdbcEnvironment jdbcEnvironment) {
    return new SequenceStructure(jdbcEnvironment, sequenceName, initialValue, 1,
        type.getReturnedClass());
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
      final Identifier catalog = jdbcEnvironment.getIdentifierHelper().toIdentifier(
          ConfigurationHelper.getString(CATALOG, params)
      );
      final Identifier schema = jdbcEnvironment.getIdentifierHelper().toIdentifier(
          ConfigurationHelper.getString(SCHEMA, params)
      );
      return new QualifiedSequenceName(catalog, schema,
          jdbcEnvironment.getIdentifierHelper().toIdentifier(sequenceName));
    }
  }

  private static int determineFetchSize(Properties params) {
    int fetchSize = ConfigurationHelper.getInt(FETCH_SIZE_PARAM, params, DEFAULT_FETCH_SIZE);
    if (fetchSize <= 0) {
      throw new MappingException("fetch size must be positive");
    }
    return fetchSize;
  }

  private static int determineInitialValue(Properties params) {
    int initialValue = ConfigurationHelper.getInt(INITIAL_VALUE_PARAM, params,
        DEFAULT_INITIAL_VALUE);
    if (initialValue <= 0) {
      throw new MappingException("initial value must be positive");
    }
    return initialValue;
  }

  @Override
  public void configure(Type type, Properties params, ServiceRegistry serviceRegistry)
      throws MappingException {
    JdbcEnvironment jdbcEnvironment = serviceRegistry.getService(JdbcEnvironment.class);
    this.sequenceName = determineSequenceName(jdbcEnvironment, params);
    this.fetchSize = determineFetchSize(params);
    int initialValue = determineInitialValue(params);
    this.select = buildSelect(jdbcEnvironment.getDialect(), sequenceName, fetchSize);
    this.databaseStructure = buildDatabaseStructure(type, sequenceName, initialValue,
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
    try {
      // Use a separate connection to get new sequence values. This ensures that it also uses a
      // separate read/write transaction, which again means that it will not interfere with any
      // retries of the actual business transaction.
      connection = session.getJdbcConnectionAccess().obtainConnection();
      connection.createStatement().execute("begin");
      connection.createStatement().execute("set transaction read write");
      List<Long> identifiers = new ArrayList<>(this.fetchSize);
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery(this.select)) {
        while (resultSet.next()) {
          identifiers.add(resultSet.getLong(1));
        }
      }
      return identifiers.iterator();
    } catch (SQLException sqlException) {
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
        try {
          session.getJdbcConnectionAccess().releaseConnection(connection);
        } catch (SQLException ignore) {
          // ignore any errors during release of the connection.
        }
      }
    }
  }

  @Override
  public String toString() {
    return getSequenceName();
  }

}
