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

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.internal.StandardForeignKeyExporter;

/**
 * Produces the Foreign Key DDL strings for Hibernate table creation.
 */
public class SpannerForeignKeyExporter extends StandardForeignKeyExporter {

  private SpannerDatabaseInfo spannerDatabaseInfo;

  public SpannerForeignKeyExporter(Dialect dialect) {
    super(dialect);
  }

  public void init(SpannerDatabaseInfo spannerDatabaseInfo) {
    this.spannerDatabaseInfo = spannerDatabaseInfo;
  }

  @Override
  public String[] getSqlDropStrings(ForeignKey foreignKey, Metadata metadata,
      SqlStringGenerationContext context) {
    if (spannerDatabaseInfo == null) {
      throw new IllegalStateException(
          "Cannot determine which foreign keys to drop because spannerDatabaseInfo was null.");
    }

    if (foreignKeyExists(foreignKey)) {
      return super.getSqlDropStrings(foreignKey, metadata, context);
    } else {
      return new String[0];
    }
  }

  private boolean foreignKeyExists(ForeignKey foreignKey) {
    Table table = foreignKey.getTable();
    return spannerDatabaseInfo.getAllTables().contains(table)
        && spannerDatabaseInfo.getImportedForeignKeys(table).contains(foreignKey.getName());
  }
}
