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

/**
 * {@link SpannerExtractionContext} uses a separate JDBC connection for extracting metadata from the
 * database. This ensures that no queries are executed on the connection used by the {@link
 * SpannerSchemaManagementTool} while that connection is in a DDL batch.
 */
public class SpannerExtractionContext extends ImprovedExtractionContextImpl {
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
      }
    } catch (SQLException ignore) {
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
        extractionConnection.close();
      } catch (SQLException ignore) {
        // Nothing we can do, just ignore it.
      }
    }
  }
}
