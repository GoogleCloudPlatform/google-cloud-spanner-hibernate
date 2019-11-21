/*
 * Copyright 2019 Google LLC
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

import com.google.cloud.spanner.hibernate.Interleaved;
import com.google.cloud.spanner.hibernate.SchemaUtils;
import com.google.cloud.spanner.hibernate.SpannerTableExporter;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;

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

  public void createTablesInit(ExecutionOptions options, Metadata metadata) {
    getSpannerTableExporter(options).initializeDependencies(buildCreateTableDependencies(metadata));
  }

  public void dropTablesInit(ExecutionOptions options, Metadata metadata) {
    getSpannerTableExporter(options).initializeDependencies(buildDropTableDependencies(metadata));
  }

  private SpannerTableExporter getSpannerTableExporter(ExecutionOptions options) {
    JdbcContext jdbcContext = this.resolveJdbcContext(options.getConfigurationValues());
    return (SpannerTableExporter) jdbcContext.getDialect().getTableExporter();
  }

  /**
   * Returns a {@link Map} which maps a table to the table it is interleaved with.
   */
  private Map<Table, Table> buildCreateTableDependencies(Metadata metadata) {
    HashMap<Table, Table> interleaveDependencies = new HashMap<>();

    for (Table table : metadata.collectTableMappings()) {
      Interleaved interleaved = SchemaUtils.getInterleaveAnnotation(table, metadata);
      if (interleaved != null) {
        interleaveDependencies.put(table, SchemaUtils.getTable(interleaved.parent(), metadata));
      }
    }

    return interleaveDependencies;
  }

  /**
   * Returns a {@link Map} which maps a table to the table that must be dropped before it.
   */
  private static Map<Table, Table> buildDropTableDependencies(Metadata metadata) {
    HashMap<Table, Table> dropTableDependencies = new HashMap<>();

    for (Table table : metadata.collectTableMappings()) {
      Interleaved interleaved = SchemaUtils.getInterleaveAnnotation(table, metadata);
      if (interleaved != null) {
        dropTableDependencies.put(SchemaUtils.getTable(interleaved.parent(), metadata), table);
      }
    }

    return dropTableDependencies;
  }
}
