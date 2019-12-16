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
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

/**
 * A modified version of the {@link SchemaCreatorImpl} which batches DDL statements
 * to optimize performance.
 */
public class SpannerSchemaCreator implements SchemaCreator {

  private final SpannerSchemaManagementTool tool;
  private final SchemaCreator schemaCreator;

  public SpannerSchemaCreator(SpannerSchemaManagementTool tool, SchemaCreator schemaCreator) {
    this.tool = tool;
    this.schemaCreator = schemaCreator;
  }

  @Override
  public void doCreation(
      Metadata metadata,
      ExecutionOptions options,
      SourceDescriptor sourceDescriptor,
      TargetDescriptor targetDescriptor) {

    // Add auxiliary database objects to batch DDL statements
    metadata.getDatabase().addAuxiliaryDatabaseObject(new StartBatchDdl(Action.CREATE));
    metadata.getDatabase().addAuxiliaryDatabaseObject(new RunBatchDdl(Action.CREATE));

    // Initialize exporters with interleave dependencies so tables are created in the right order.
    tool.getSpannerTableExporter(options).initializeTableExporter(
        metadata, tool.getDatabaseMetaData(options), Action.CREATE);

    schemaCreator.doCreation(metadata, options, sourceDescriptor, targetDescriptor);
  }
}
