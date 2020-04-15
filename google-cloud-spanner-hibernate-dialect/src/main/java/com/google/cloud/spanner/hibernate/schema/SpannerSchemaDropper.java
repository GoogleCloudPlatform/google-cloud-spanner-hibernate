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

import java.sql.Connection;
import java.sql.SQLException;
import org.hibernate.boot.Metadata;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.spi.DelayedDropAction;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

/**
 * A modified version of the {@link SchemaDropperImpl} which batches DDL statements
 * to optimize performance.
 */
public class SpannerSchemaDropper implements SchemaDropper {

  private final SpannerSchemaManagementTool tool;
  private final SchemaDropper schemaDropper;

  public SpannerSchemaDropper(SpannerSchemaManagementTool tool, SchemaDropper schemaDropper) {
    this.tool = tool;
    this.schemaDropper = schemaDropper;
  }

  @Override
  public void doDrop(
      Metadata metadata,
      ExecutionOptions options,
      SourceDescriptor sourceDescriptor,
      TargetDescriptor targetDescriptor) {

    // Initialize auxiliary database objects to enable DDL statement batching.
    metadata.getDatabase().addAuxiliaryDatabaseObject(new StartBatchDdl(Action.DROP));
    metadata.getDatabase().addAuxiliaryDatabaseObject(new RunBatchDdl(Action.DROP));

    try (Connection connection = tool.getDatabaseMetadataConnection(options)) {
      // Initialize exporters with drop table dependencies so tables are dropped in the right order.
      SpannerDatabaseInfo spannerDatabaseInfo = new SpannerDatabaseInfo(connection.getMetaData());
      tool.getSpannerTableExporter(options).init(metadata, spannerDatabaseInfo, Action.DROP);
      schemaDropper.doDrop(metadata, options, sourceDescriptor, targetDescriptor);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to update Spanner table schema.", e);
    }
  }

  @Override
  public DelayedDropAction buildDelayedAction(
      Metadata metadata, ExecutionOptions options, SourceDescriptor sourceDescriptor) {

    try (Connection connection = tool.getDatabaseMetadataConnection(options)) {
      // Initialize exporters with drop table dependencies so tables are dropped in the right order.
      SpannerDatabaseInfo spannerDatabaseInfo = new SpannerDatabaseInfo(connection.getMetaData());
      tool.getSpannerTableExporter(options).init(
          metadata, spannerDatabaseInfo, Action.DROP);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to update Spanner table schema.", e);
    }

    return schemaDropper.buildDelayedAction(metadata, options, sourceDescriptor);
  }
}
