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
import java.util.Map;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.ExecutionOptions;
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
    private final Method createStatementMethod;
    private final Method executeMethod;
    private final DdlTransactionIsolator delegate;

    SpannerDdlTransactionIsolator(DdlTransactionIsolator delegate) throws NoSuchMethodException {
      this.delegate = delegate;
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
      // Create a proxy for the connection that will override the call to
      // Connection#createStatement().
      return (Connection)
          Proxy.newProxyInstance(
              delegateConnection.getClass().getClassLoader(),
              new Class[] {Connection.class},
              (proxy, method, args) -> {
                // Only handle the Connection#createStatement() differently.
                // All other methods are just passed through.
                if (method.equals(createStatementMethod)) {
                  // Create a proxy for the returned Statement that will override the behavior of
                  // Statement#execute(String).
                  Statement delegateStatement = delegateConnection.createStatement();
                  return Proxy.newProxyInstance(
                      delegateConnection.getClass().getClassLoader(),
                      new Class[] {Statement.class},
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
                          if (statement.getType() == StatementType.CLIENT_SIDE) {
                            if (statement.getClientSideStatementType()
                                    == ClientSideStatementType.START_BATCH_DDL
                                || statement.getClientSideStatementType()
                                    == ClientSideStatementType.RUN_BATCH) {
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
                        }
                        try {
                          return method1.invoke(delegateStatement, args1);
                        } catch (InvocationTargetException e) {
                          throw e.getTargetException();
                        }
                      });
                }
                try {
                  return method.invoke(delegateConnection, args);
                } catch (InvocationTargetException e) {
                  throw e.getTargetException();
                }
              });
    }

    @Override
    public void release() {
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
