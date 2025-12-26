/*
 * Copyright 2019-2023 Google LLC
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
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

/**
 * A modified version of the {@link SchemaCreatorImpl} which batches DDL statements to optimize
 * performance.
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
      ContributableMatcher contributableInclusionFilter,
      SourceDescriptor sourceDescriptor,
      TargetDescriptor targetDescriptor) {

    // Add auxiliary database objects to batch DDL statements
    metadata.getDatabase().addAuxiliaryDatabaseObject(new StartBatchDdl(Action.CREATE));
    metadata.getDatabase().addAuxiliaryDatabaseObject(new RunBatchDdl(Action.CREATE));

    DdlTransactionIsolator isolator = tool.getDdlTransactionIsolator(options);
    try {
      Connection connection = isolator.getIsolatedConnection();

      String defaultSchema =
          (String) options.getConfigurationValues().get(AvailableSettings.DEFAULT_SCHEMA);

      SpannerDatabaseInfo spannerDatabaseInfo =
          new SpannerDatabaseInfo(metadata.getDatabase(), connection.getMetaData(), defaultSchema);

      tool.getSpannerTableExporter(options).init(metadata, spannerDatabaseInfo, Action.CREATE);
      tool.getForeignKeyExporter(options).init(spannerDatabaseInfo);
      schemaCreator.doCreation(
          metadata, options, contributableInclusionFilter, sourceDescriptor, targetDescriptor);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to update Spanner table schema.", e);
    } finally {
      isolator.release();
    }
  }
}
