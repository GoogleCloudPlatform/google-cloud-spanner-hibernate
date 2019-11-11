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
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

/**
 * A modified version of the {@link SchemaDropperImpl} which batches DDL statements
 * to optimize performance.
 */
public class SpannerSchemaDropper extends SchemaDropperImpl {

  public SpannerSchemaDropper(HibernateSchemaManagementTool tool, SchemaFilter schemaFilter) {
    super(tool, schemaFilter);
  }

  @Override
  public void doDrop(
      Metadata metadata,
      ExecutionOptions options,
      SourceDescriptor sourceDescriptor,
      TargetDescriptor targetDescriptor) {
    metadata.getDatabase().addAuxiliaryDatabaseObject(new StartBatchDdl());
    metadata.getDatabase().addAuxiliaryDatabaseObject(new RunBatchDdl());

    super.doDrop(metadata, options, sourceDescriptor, targetDescriptor);
  }
}
