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

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.spi.ExtractionContext.DatabaseObjectAccess;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.junit.Test;

/** Tests for {@link SpannerExtractionContext}. */
public class SpannerExtractionContextTest {

  @Test
  public void testGetJdbcConnection_obtainsNewConnection() throws SQLException {
    DdlTransactionIsolator ddlTransactionIsolator = mock(DdlTransactionIsolator.class);
    JdbcContext jdbcContext = mock(JdbcContext.class);
    JdbcConnectionAccess jdbcConnectionAccess = mock(JdbcConnectionAccess.class);
    Connection connection = mock(Connection.class);
    when(ddlTransactionIsolator.getJdbcContext()).thenReturn(jdbcContext);
    when(jdbcContext.getJdbcConnectionAccess()).thenReturn(jdbcConnectionAccess);

    when(jdbcConnectionAccess.obtainConnection()).thenReturn(connection);
    SpannerExtractionContext context =
        new SpannerExtractionContext(
            mock(ServiceRegistry.class),
            mock(JdbcEnvironment.class),
            mock(SqlStringGenerationContext.class),
            ddlTransactionIsolator,
            mock(DatabaseObjectAccess.class));

    assertSame(connection, context.getJdbcConnection());
  }

  @Test
  public void testGetJdbcConnection_fallbacksToDdlConnectionOnException() throws SQLException {
    DdlTransactionIsolator ddlTransactionIsolator = mock(DdlTransactionIsolator.class);
    JdbcContext jdbcContext = mock(JdbcContext.class);
    JdbcConnectionAccess jdbcConnectionAccess = mock(JdbcConnectionAccess.class);
    Connection ddlConnection = mock(Connection.class);
    when(ddlTransactionIsolator.getJdbcContext()).thenReturn(jdbcContext);
    when(jdbcContext.getJdbcConnectionAccess()).thenReturn(jdbcConnectionAccess);
    when(ddlTransactionIsolator.getIsolatedConnection()).thenReturn(ddlConnection);

    when(jdbcConnectionAccess.obtainConnection()).thenThrow(new SQLException("test"));
    SpannerExtractionContext context =
        new SpannerExtractionContext(
            mock(ServiceRegistry.class),
            mock(JdbcEnvironment.class),
            mock(SqlStringGenerationContext.class),
            ddlTransactionIsolator,
            mock(DatabaseObjectAccess.class));

    assertSame(ddlConnection, context.getJdbcConnection());
  }

  @Test
  public void testCleanupReleasesConnection() throws SQLException {
    DdlTransactionIsolator ddlTransactionIsolator = mock(DdlTransactionIsolator.class);
    JdbcContext jdbcContext = mock(JdbcContext.class);
    JdbcConnectionAccess jdbcConnectionAccess = mock(JdbcConnectionAccess.class);
    Connection connection = mock(Connection.class);
    when(ddlTransactionIsolator.getJdbcContext()).thenReturn(jdbcContext);
    when(jdbcContext.getJdbcConnectionAccess()).thenReturn(jdbcConnectionAccess);

    when(jdbcConnectionAccess.obtainConnection()).thenReturn(connection);
    SpannerExtractionContext context =
        new SpannerExtractionContext(
            mock(ServiceRegistry.class),
            mock(JdbcEnvironment.class),
            mock(SqlStringGenerationContext.class),
            ddlTransactionIsolator,
            mock(DatabaseObjectAccess.class));

    // Ensure that we initialize a connection.
    context.getJdbcConnection();
    context.cleanup();

    verify(jdbcConnectionAccess).releaseConnection(connection);
  }

  @Test
  public void testCleanupIsNoOpWithoutConnection() throws SQLException {
    DdlTransactionIsolator ddlTransactionIsolator = mock(DdlTransactionIsolator.class);
    JdbcContext jdbcContext = mock(JdbcContext.class);
    JdbcConnectionAccess jdbcConnectionAccess = mock(JdbcConnectionAccess.class);
    Connection connection = mock(Connection.class);
    when(ddlTransactionIsolator.getJdbcContext()).thenReturn(jdbcContext);
    when(jdbcContext.getJdbcConnectionAccess()).thenReturn(jdbcConnectionAccess);

    when(jdbcConnectionAccess.obtainConnection()).thenReturn(connection);
    SpannerExtractionContext context =
        new SpannerExtractionContext(
            mock(ServiceRegistry.class),
            mock(JdbcEnvironment.class),
            mock(SqlStringGenerationContext.class),
            ddlTransactionIsolator,
            mock(DatabaseObjectAccess.class));

    // Cleaning up without ever having used the context should be no-op.
    context.cleanup();

    verify(connection, never()).close();
  }

  @Test
  public void testCleanupIgnoresException() throws SQLException {
    DdlTransactionIsolator ddlTransactionIsolator = mock(DdlTransactionIsolator.class);
    JdbcContext jdbcContext = mock(JdbcContext.class);
    JdbcConnectionAccess jdbcConnectionAccess = mock(JdbcConnectionAccess.class);
    Connection connection = mock(Connection.class);
    when(ddlTransactionIsolator.getJdbcContext()).thenReturn(jdbcContext);
    when(jdbcContext.getJdbcConnectionAccess()).thenReturn(jdbcConnectionAccess);
    when(jdbcConnectionAccess.obtainConnection()).thenReturn(connection);
    doThrow(new SQLException("test")).when(jdbcConnectionAccess).releaseConnection(connection);
    SpannerExtractionContext context =
        new SpannerExtractionContext(
            mock(ServiceRegistry.class),
            mock(JdbcEnvironment.class),
            mock(SqlStringGenerationContext.class),
            ddlTransactionIsolator,
            mock(DatabaseObjectAccess.class));

    context.getJdbcConnection();
    // Cleanup should ignore any errors.
    context.cleanup();
    verify(jdbcConnectionAccess).releaseConnection(connection);
  }
}
