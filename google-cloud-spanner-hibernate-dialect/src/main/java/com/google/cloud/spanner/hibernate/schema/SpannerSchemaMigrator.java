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
import org.hibernate.boot.Metadata;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.TargetDescriptor;

/**
 * A wrapper around the {@link SchemaMigrator} which initializes the Spanner table exporter
 * before performing the schema migration.
 *
 * @since 1.1
 */
public class SpannerSchemaMigrator implements SchemaMigrator {

  private final SpannerSchemaManagementTool tool;
  private final SchemaMigrator schemaMigrator;

  public SpannerSchemaMigrator(SpannerSchemaManagementTool tool, SchemaMigrator schemaMigrator) {
    this.tool = tool;
    this.schemaMigrator = schemaMigrator;
  }

  @Override
  public void doMigration(
      Metadata metadata, ExecutionOptions options, TargetDescriptor targetDescriptor) {

    // Add auxiliary database objects to batch DDL statements
    metadata.getDatabase().addAuxiliaryDatabaseObject(new StartBatchDdl(Action.UPDATE));
    metadata.getDatabase().addAuxiliaryDatabaseObject(new RunBatchDdl(Action.UPDATE));

    try (Connection connection = tool.getDatabaseMetadataConnection(options)) {
      SpannerDatabaseInfo spannerDatabaseInfo = new SpannerDatabaseInfo(connection.getMetaData());
      tool.getSpannerTableExporter(options).init(metadata, spannerDatabaseInfo, Action.UPDATE);
      tool.getForeignKeyExporter(options).init(spannerDatabaseInfo);
      schemaMigrator.doMigration(metadata, options, targetDescriptor);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to update Spanner table schema.", e);
    }
  }
}
