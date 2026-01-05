/*
 * Copyright 2019-2025 Google LLC
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

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

/** Hibernate implementer which generates unique index strings in DDL statements. */
public class SpannerUniqueDelegate implements UniqueDelegate {

  protected final Dialect dialect;

  /**
   * Constructs the Spanner unique delegate responsible for generating statements for building
   * Unique indices.
   *
   * @param dialect The dialect for which we are handling unique constraints
   */
  public SpannerUniqueDelegate(Dialect dialect) {
    this.dialect = dialect;
  }

  @Override
  public String getColumnDefinitionUniquenessFragment(
      Column column, SqlStringGenerationContext sqlStringGenerationContext) {
    return "";
  }

  @Override
  public String getTableCreationUniqueConstraintsFragment(
      Table table, SqlStringGenerationContext sqlStringGenerationContext) {
    return "";
  }

  @Override
  public String getAlterTableToAddUniqueKeyCommand(
      UniqueKey uniqueKey,
      Metadata metadata,
      SqlStringGenerationContext sqlStringGenerationContext) {
    final String tableName =
        sqlStringGenerationContext.format(uniqueKey.getTable().getQualifiedTableName());

    // Correctly qualify the index name with the schema
    String indexName = getQualifiedIndexName(uniqueKey, sqlStringGenerationContext);

    StringBuilder statement =
        new StringBuilder(dialect.getCreateIndexString(true))
            .append(" ")
            .append(indexName)
            .append(" on ")
            .append(tableName)
            .append(" (");
    boolean first = true;
    for (Column column : uniqueKey.getColumns()) {
      if (first) {
        first = false;
      } else {
        statement.append(", ");
      }
      statement.append(column.getQuotedName(dialect));
      if (uniqueKey.getColumnOrderMap().containsKey(column)) {
        statement.append(" ").append(uniqueKey.getColumnOrderMap().get(column));
      }
    }
    statement.append(")");
    statement.append(dialect.getCreateIndexTail(true, uniqueKey.getColumns()));
    return statement.toString();
  }

  @Override
  public String getAlterTableToDropUniqueKeyCommand(
      UniqueKey uniqueKey,
      Metadata metadata,
      SqlStringGenerationContext sqlStringGenerationContext) {
    // Dropping a unique constraint in Spanner is equivalent to dropping the index.
    String indexName = getQualifiedIndexName(uniqueKey, sqlStringGenerationContext);
    return "drop index " + indexName;
  }

  /** Generates the qualified index name (e.g. `schema`.`indexName`) using the context. */
  private String getQualifiedIndexName(UniqueKey uniqueKey, SqlStringGenerationContext context) {
    Table table = uniqueKey.getTable();
    String name = uniqueKey.getName();

    if (table.getSchema() != null || table.getCatalog() != null) {
      QualifiedTableName qualifiedIndexName =
          new QualifiedTableName(
              Identifier.toIdentifier(table.getCatalog()),
              Identifier.toIdentifier(table.getSchema()),
              Identifier.toIdentifier(name));
      return context.format(qualifiedIndexName);
    } else {
      return dialect.quote(name);
    }
  }
}
