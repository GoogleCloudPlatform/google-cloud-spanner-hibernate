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

import com.google.cloud.spanner.hibernate.SpannerTableExporter;
import java.sql.Connection;
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
