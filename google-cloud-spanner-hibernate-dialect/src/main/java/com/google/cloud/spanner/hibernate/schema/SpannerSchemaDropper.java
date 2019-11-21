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

import org.hibernate.boot.Metadata;
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
    metadata.getDatabase().addAuxiliaryDatabaseObject(new StartBatchDdl());
    metadata.getDatabase().addAuxiliaryDatabaseObject(new RunBatchDdl());

    // Initialize exporters with drop table dependencies so tables are dropped in the right order.
    tool.getSpannerTableExporter(options).initializeTableExporter(metadata, false);

    schemaDropper.doDrop(metadata, options, sourceDescriptor, targetDescriptor);
  }

  @Override
  public DelayedDropAction buildDelayedAction(
      Metadata metadata, ExecutionOptions options, SourceDescriptor sourceDescriptor) {
    return schemaDropper.buildDelayedAction(metadata, options, sourceDescriptor);
  }
}
