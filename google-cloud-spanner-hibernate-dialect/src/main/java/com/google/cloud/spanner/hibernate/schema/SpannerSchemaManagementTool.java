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

package com.google.cloud.spanner.hibernate.schema;

import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.connection.AbstractStatementParser;
import com.google.cloud.spanner.connection.AbstractStatementParser.ParsedStatement;
import com.google.cloud.spanner.connection.AbstractStatementParser.StatementType;
import com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType;
import com.google.cloud.spanner.hibernate.SpannerTableExporter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.internal.InformationExtractorJdbcDatabaseMetaDataImpl;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.ExtractionTool;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaMigrator;

/**
 * The custom implementation of {@link HibernateSchemaManagementTool} for Spanner to support batched
 * DDL statements.
 */
public class SpannerSchemaManagementTool extends HibernateSchemaManagementTool {

  /**
   * Custom implementation for {@link DdlTransactionIsolator} that will automatically create a proxy
   * for Connection and Statement. These proxies will again be used to override the default behavior
   * of the `START BATCH DDL` and `RUN BATCH` statements, by converting any {@link SQLException}
   * that is returned by these methods into a {@link SpannerExceptionFactory}. The reason for this
   * is that `START BATCH DDL` and `RUN BATCH` are added to each schema migration as auxiliary
   * database objects automatically by the {@link SpannerSchemaManagementTool}. Hibernate will
   * however silently ignore any {@link SQLException} that is thrown for auxiliary database objects.
   * This means that if for example `RUN BATCH` fails, Hibernate will still report success for the
   * entire migration. Throwing a {@link com.google.cloud.spanner.SpannerException} instead does
   * cause an error to be returned for the migration.
   */
  static class SpannerDdlTransactionIsolator implements DdlTransactionIsolator {

    private static final AbstractStatementParser PARSER =
        AbstractStatementParser.getInstance(Dialect.GOOGLE_STANDARD_SQL);

    /**
     * A set containing all connections that have been obtained by this
     * {@link DdlTransactionIsolator}. We use a {@link Set}, because the connection is obtained from
     * the {@link DdlTransactionIsolator} that is configured for this environment, and some
     * implementations
     * ({@link
     * org.hibernate.resource.transaction.backend.jta.internal.DdlTransactionIsolatorJtaImpl})
     * return the same connection each time, and we only want to release it once.
     */
    private final Set<Connection> obtainedConnections = new HashSet<>();
    private final Method connectionCloseMethod;
    private final Method createStatementMethod;
    private final Method executeMethod;
    private final DdlTransactionIsolator delegate;

    SpannerDdlTransactionIsolator(DdlTransactionIsolator delegate) throws NoSuchMethodException {
      this.delegate = delegate;
      this.connectionCloseMethod = Connection.class.getDeclaredMethod("close");
      this.createStatementMethod = Connection.class.getDeclaredMethod("createStatement");
      this.executeMethod = Statement.class.getDeclaredMethod("execute", String.class);
    }

    @Override
    public JdbcContext getJdbcContext() {
      return delegate.getJdbcContext();
    }

    @Override
    public void prepare() {
      delegate.prepare();
    }

    @Override
    public Connection getIsolatedConnection() {
      Connection delegateConnection = this.delegate.getIsolatedConnection();
      // Add the delegate connection to the set of obtained connections so we can release these
      // when the DdlTransactionIsolator is released.
      obtainedConnections.add(delegateConnection);
      // Create a proxy for the connection that will override the call to
      // Connection#createStatement().
      return (Connection)
          Proxy.newProxyInstance(
              delegateConnection.getClass().getClassLoader(),
              new Class[]{Connection.class},
              (proxy, method, args) -> {
                // Only handle the Connection#createStatement() differently.
                // All other methods are just passed through.
                if (method.equals(createStatementMethod)) {
                  // Create a proxy for the returned Statement that will override the behavior of
                  // Statement#execute(String).
                  return createProxyStatement(delegateConnection);
                } else if (method.equals(connectionCloseMethod)) {
                  // Ignore as the connection is released when this DdlTransactionIsolator is
                  // released.
                  return null;
                }
                try {
                  return method.invoke(delegateConnection, args);
                } catch (InvocationTargetException e) {
                  throw e.getTargetException();
                }
              });
    }

    /**
     * Creates a proxy for a {@link Statement} that will throw a
     * {@link com.google.cloud.spanner.SpannerException} instead of a {@link SQLException} if a
     * `START BATCH DDL` or `RUN BATCH` statement fails.
     */
    private Statement createProxyStatement(Connection delegateConnection) throws SQLException {
      Statement delegateStatement = delegateConnection.createStatement();
      return (Statement)
          Proxy.newProxyInstance(
              delegateConnection.getClass().getClassLoader(),
              new Class[]{Statement.class},
              (proxy1, method1, args1) -> {
                // Only handle the Statement#execute(String) method differently.
                // All other methods are just passed through.
                if (method1.equals(executeMethod)
                    && args1 != null
                    && args1.length == 1
                    && args1[0] instanceof String) {
                  // Check if the statement that is being executed is either `START BATCH
                  // DDL` or `RUN BATCH`.
                  String sql = (String) args1[0];
                  ParsedStatement statement =
                      PARSER.parse(com.google.cloud.spanner.Statement.of(sql));
                  if (statement.getType() == StatementType.CLIENT_SIDE
                      && (statement.getClientSideStatementType()
                      == ClientSideStatementType.START_BATCH_DDL
                      || statement.getClientSideStatementType()
                      == ClientSideStatementType.RUN_BATCH)) {
                    try {
                      // Try to execute the statement, and convert any SQLException to a
                      // SpannerException.
                      return method1.invoke(delegateStatement, args1);
                    } catch (InvocationTargetException exception) {
                      if (exception.getTargetException() instanceof SQLException) {
                        throw SpannerExceptionFactory.newSpannerException(
                            exception.getTargetException());
                      }
                      throw exception.getTargetException();
                    }
                  }
                }
                try {
                  return method1.invoke(delegateStatement, args1);
                } catch (InvocationTargetException e) {
                  throw e.getTargetException();
                }
              });
    }

    @Override
    public void release() {
      for (Connection connection : obtainedConnections) {
        try {
          getJdbcContext().getJdbcConnectionAccess().releaseConnection(connection);
        } catch (SQLException ignore) {
          // Ignore any errors when releasing a connection and continue with the next.
        }
      }
      delegate.release();
    }
  }

  @Override
  public SchemaCreator getSchemaCreator(Map options) {
    return new SpannerSchemaCreator(this, super.getSchemaCreator(options));
  }

  @Override
  public SchemaDropper getSchemaDropper(Map options) {
    return new SpannerSchemaDropper(this, super.getSchemaDropper(options));
  }

  @Override
  public SchemaMigrator getSchemaMigrator(Map options) {
    return new SpannerSchemaMigrator(this, super.getSchemaMigrator(options));
  }

  @Override
  public DdlTransactionIsolator getDdlTransactionIsolator(JdbcContext jdbcContext) {
    DdlTransactionIsolator delegate = super.getDdlTransactionIsolator(jdbcContext);
    try {
      return new SpannerDdlTransactionIsolator(delegate);
    } catch (Throwable ignore) {
      return delegate;
    }
  }

  @Override
  public ExtractionTool getExtractionTool() {
    return SpannerExtractionTool.INSTANCE;
  }

  /**
   * {@link SpannerExtractionTool} creates an {@link ExtractionContext} that uses a separate JDBC
   * connection for extracting additional metadata from the database. This prevents queries from
   * being executed on the connection that is executing the DDL batch for the migration.
   */
  private static class SpannerExtractionTool implements ExtractionTool {

    private static final SpannerExtractionTool INSTANCE = new SpannerExtractionTool();

    private SpannerExtractionTool() {
    }

    @Override
    public ExtractionContext createExtractionContext(
        ServiceRegistry serviceRegistry,
        JdbcEnvironment jdbcEnvironment,
        SqlStringGenerationContext sqlStringGenerationContext,
        DdlTransactionIsolator ddlTransactionIsolator,
        ExtractionContext.DatabaseObjectAccess databaseObjectAccess) {
      return new SpannerExtractionContext(
          serviceRegistry,
          jdbcEnvironment,
          sqlStringGenerationContext,
          ddlTransactionIsolator,
          databaseObjectAccess);
    }

    @Override
    public InformationExtractor createInformationExtractor(ExtractionContext extractionContext) {
      return new InformationExtractorJdbcDatabaseMetaDataImpl(extractionContext);
    }
  }

  SpannerTableExporter getSpannerTableExporter(ExecutionOptions options) {
    JdbcContext jdbcContext = this.resolveJdbcContext(options.getConfigurationValues());
    return (SpannerTableExporter) jdbcContext.getDialect().getTableExporter();
  }

  SpannerForeignKeyExporter getForeignKeyExporter(ExecutionOptions options) {
    JdbcContext jdbcContext = this.resolveJdbcContext(options.getConfigurationValues());
    return (SpannerForeignKeyExporter) jdbcContext.getDialect().getForeignKeyExporter();
  }

  Connection getDatabaseMetadataConnection(ExecutionOptions options) {
    JdbcContext jdbcContext = this.resolveJdbcContext(options.getConfigurationValues());
    DdlTransactionIsolator ddlTransactionIsolator = this.getDdlTransactionIsolator(jdbcContext);
    return ddlTransactionIsolator.getIsolatedConnection();
  }
}
