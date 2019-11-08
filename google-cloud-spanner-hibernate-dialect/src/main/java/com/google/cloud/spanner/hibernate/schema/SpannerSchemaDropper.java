package com.google.cloud.spanner.hibernate.schema;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
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
