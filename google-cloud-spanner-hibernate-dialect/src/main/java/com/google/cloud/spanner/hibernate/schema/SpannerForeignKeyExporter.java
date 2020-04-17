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
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.tool.schema.internal.StandardForeignKeyExporter;

public class SpannerForeignKeyExporter extends StandardForeignKeyExporter {

  private SpannerDatabaseInfo spannerDatabaseInfo;

  public SpannerForeignKeyExporter(Dialect dialect) {
    super(dialect);
  }

  public void init(SpannerDatabaseInfo spannerDatabaseInfo) {
    this.spannerDatabaseInfo = spannerDatabaseInfo;
  }

  @Override
  public String[] getSqlDropStrings(ForeignKey foreignKey, Metadata metadata) {
    if (spannerDatabaseInfo == null || foreignKeyExists(foreignKey)) {
      return super.getSqlDropStrings(foreignKey, metadata);
    } else {
      return new String[0];
    }
  }

  private boolean foreignKeyExists(ForeignKey foreignKey) {
    String table = foreignKey.getTable().getName();
    return spannerDatabaseInfo.getAllTables().contains(table)
        && spannerDatabaseInfo.getImportedForeignKeys(table).contains(foreignKey.getName());
  }
}
