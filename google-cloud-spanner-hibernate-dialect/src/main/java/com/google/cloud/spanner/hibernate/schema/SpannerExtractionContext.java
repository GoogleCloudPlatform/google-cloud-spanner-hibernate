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

import java.sql.Connection;
import java.sql.SQLException;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.internal.exec.ImprovedExtractionContextImpl;
import org.jboss.logging.Logger;

/**
 * {@link SpannerExtractionContext} uses a separate JDBC connection for extracting metadata from the
 * database. This ensures that no queries are executed on the connection used by the {@link
 * SpannerSchemaManagementTool} while that connection is in a DDL batch.
 */
public class SpannerExtractionContext extends ImprovedExtractionContextImpl {
  private static final Logger log = Logger.getLogger(SpannerExtractionContext.class);
  private final JdbcConnectionAccess jdbcConnectionAccess;
  private Connection extractionConnection;

  SpannerExtractionContext(
      ServiceRegistry serviceRegistry,
      JdbcEnvironment jdbcEnvironment,
      SqlStringGenerationContext sqlStringGenerationContext,
      DdlTransactionIsolator ddlTransactionIsolator,
      DatabaseObjectAccess databaseObjectAccess) {
    super(
        serviceRegistry,
        jdbcEnvironment,
        sqlStringGenerationContext,
        ddlTransactionIsolator,
        databaseObjectAccess);
    this.jdbcConnectionAccess = ddlTransactionIsolator.getJdbcContext().getJdbcConnectionAccess();
  }

  @Override
  public Connection getJdbcConnection() {
    // Get a separate JDBC connection for metadata extraction. This makes sure that Hibernate does
    // not try to extract metadata using a connection that has an active DDL batch.
    try {
      if (extractionConnection == null) {
        extractionConnection = jdbcConnectionAccess.obtainConnection();
        extractionConnection.setAutoCommit(true);
      }
    } catch (SQLException exception) {
      log.warn(
          "An exception was thrown while obtaining a JDBC connection for metadata extraction. "
              + "Falling back to the default connection used for DDL execution.",
          exception);
      // Fallback and use the original connection if anything goes wrong.
      extractionConnection = super.getJdbcConnection();
    }
    return extractionConnection;
  }

  @Override
  public void cleanup() {
    super.cleanup();
    if (extractionConnection != null) {
      try {
        if (extractionConnection.getAutoCommit()) {
          extractionConnection.setAutoCommit(false);
        }
        jdbcConnectionAccess.releaseConnection(extractionConnection);
      } catch (SQLException exception) {
        log.warn(
            "An exception was thrown while closing the JDBC connection "
                + "that was used for metadata extraction",
            exception);
      }
    }
  }
}
