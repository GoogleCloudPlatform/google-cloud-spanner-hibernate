package com.google.cloud.spanner.hibernate.schema;

import com.google.cloud.spanner.hibernate.SpannerDialect;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.dialect.Dialect;

/**
 * Custom {@link AuxiliaryDatabaseObject} which generates the RUN BATCH statement.
 */
public class RunBatchDdl implements AuxiliaryDatabaseObject {
  private static final long serialVersionUID = 1L;

  @Override
  public String getExportIdentifier() {
    return "RUN_BATCH_DDL";
  }

  @Override
  public boolean appliesToDialect(Dialect dialect) {
    return SpannerDialect.class.isAssignableFrom(dialect.getClass());
  }

  @Override
  public boolean beforeTablesOnCreation() {
    return false;
  }

  @Override
  public String[] sqlCreateStrings(Dialect dialect) {
    return new String[] {"RUN BATCH"};
  }

  @Override
  public String[] sqlDropStrings(Dialect dialect) {
    return new String[] {"RUN BATCH"};
  }
}
