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

import com.google.cloud.spanner.hibernate.SpannerDialect;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.Action;

/** Custom {@link AuxiliaryDatabaseObject} which generates the START BATCH DDL statement. */
public class StartBatchDdl implements AuxiliaryDatabaseObject {
  private static final long serialVersionUID = 1L;

  private final Action schemaAction;

  public StartBatchDdl(Action schemaAction) {
    this.schemaAction = schemaAction;
  }

  @Override
  public String getExportIdentifier() {
    return "START_BATCH_DDL";
  }

  @Override
  public boolean appliesToDialect(Dialect dialect) {
    return SpannerDialect.class.isAssignableFrom(dialect.getClass());
  }

  @Override
  public boolean beforeTablesOnCreation() {
    return schemaAction != Action.UPDATE;
  }

  @Override
  public String[] sqlCreateStrings(SqlStringGenerationContext context) {
    return new String[] {"START BATCH DDL"};
  }

  @Override
  public String[] sqlDropStrings(SqlStringGenerationContext context) {
    if (schemaAction == Action.UPDATE) {
      return new String[] {};
    } else {
      return new String[] {"START BATCH DDL"};
    }
  }
}
