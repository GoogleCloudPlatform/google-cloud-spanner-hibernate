package com.google.cloud.spanner.hibernate.schema;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

/**
 * A modified version of the {@link SchemaCreatorImpl} which batches DDL statements
 * to optimize performance.
 */
public class SpannerSchemaCreator extends SchemaCreatorImpl {

  public SpannerSchemaCreator(
      HibernateSchemaManagementTool tool, SchemaFilter schemaFilter) {
    super(tool, schemaFilter);
  }

  @Override
  public void doCreation(
      Metadata metadata,
      ExecutionOptions options,
      SourceDescriptor sourceDescriptor,
      TargetDescriptor targetDescriptor) {
    metadata.getDatabase().addAuxiliaryDatabaseObject(new StartBatchDdl());
    metadata.getDatabase().addAuxiliaryDatabaseObject(new RunBatchDdl());

    super.doCreation(metadata, options, sourceDescriptor, targetDescriptor);
  }
}
