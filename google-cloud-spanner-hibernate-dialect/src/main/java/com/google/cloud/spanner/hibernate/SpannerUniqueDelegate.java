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

package com.google.cloud.spanner.hibernate;

import com.google.common.base.Strings;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.unique.DefaultUniqueDelegate;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.UniqueKey;

/**
 * Hibernate implementer which generates unique index strings in DDL statements.
 */
public class SpannerUniqueDelegate extends DefaultUniqueDelegate {

  /**
   * Constructs the Spanner unique delegate responsible for generating statements for building
   * Unique indices.
   *
   * @param dialect The dialect for which we are handling unique constraints
   */
  public SpannerUniqueDelegate(Dialect dialect) {
    super(dialect);
  }

  @Override
  public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata,
      SqlStringGenerationContext context) {
    return Index.buildSqlCreateIndexString(context,
        uniqueKey.getName(), uniqueKey.getTable(), uniqueKey.getColumns(),
        uniqueKey.getColumnOrderMap(), true, metadata);
  }

  @Override
  public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata,
      SqlStringGenerationContext context) {
    StringBuilder buf = new StringBuilder("DROP INDEX ");
    if (!Strings.isNullOrEmpty(uniqueKey.getTable().getSchema())) {
      buf.append(dialect.quote(uniqueKey.getTable().getSchema())).append('.');
    }
    buf.append(dialect.quote(uniqueKey.getName()));
    return buf.toString();
  }
}
