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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.spanner.hibernate.schema.SpannerDatabaseInfo;
import com.google.cloud.spanner.hibernate.schema.SpannerForeignKeyExporter;
import java.util.Collections;
import java.util.Set;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Namespace.Name;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.junit.Before;
import org.junit.Test;

/** Tests correct creation of Spanner foreign keys. */
public class SpannerForeignKeyExporterTests {

  private SpannerForeignKeyExporter spannerForeignKeyExporter;

  private SpannerDatabaseInfo spannerDatabaseInfo;

  private Metadata metadata;

  private SqlStringGenerationContext context;

  /** Setup the mocks needed for the Spanner foreign key tests. */
  @Before
  public void setup() {
    Namespace namespace = mock(Namespace.class);
    when(namespace.getPhysicalName())
        .thenReturn(new Name(Identifier.toIdentifier(""), Identifier.toIdentifier("")));
    this.spannerDatabaseInfo = mock(SpannerDatabaseInfo.class);
    Table table = new Table("orm", namespace, Identifier.toIdentifier("address"), false);
    Set<Table> tables = Collections.singleton(table);
    when(spannerDatabaseInfo.getAllTables()).thenReturn(tables);
    when(spannerDatabaseInfo.getImportedForeignKeys(table))
        .thenReturn(Collections.singleton("address_fk"));

    this.metadata = mock(Metadata.class);
    this.spannerForeignKeyExporter = new SpannerForeignKeyExporter(new SpannerDialect());
    this.spannerForeignKeyExporter.init(spannerDatabaseInfo);
    this.context = mock(SqlStringGenerationContext.class);
    when(this.context.format(any(QualifiedTableName.class)))
        .thenAnswer(
            invocation ->
                ((QualifiedTableName) invocation.getArguments()[0])
                    .getTableName()
                    .getCanonicalName());
  }

  @Test
  public void testDropForeignKey() {
    String[] dropStatements =
        spannerForeignKeyExporter.getSqlDropStrings(
            foreignKey("address", "address_fk"), metadata, this.context);
    assertThat(dropStatements).containsExactly("alter table address drop constraint address_fk");
  }

  @Test
  public void testDropMissingForeignKey_missingTable() {
    String[] dropStatements =
        spannerForeignKeyExporter.getSqlDropStrings(
            foreignKey("person", "person_fk"), metadata, this.context);
    assertThat(dropStatements).isEmpty();
  }

  @Test
  public void testDropMissingForeignKey_missingForeignKey() {
    String[] dropStatements =
        spannerForeignKeyExporter.getSqlDropStrings(
            foreignKey("address", "other_fk"), metadata, this.context);
    assertThat(dropStatements).isEmpty();
  }

  private static ForeignKey foreignKey(String tableName, String foreignKeyName) {
    ForeignKey foreignKey = new ForeignKey();
    foreignKey.setName(foreignKeyName);

    Namespace namespace = mock(Namespace.class);
    when(namespace.getPhysicalName())
        .thenReturn(new Name(Identifier.toIdentifier(""), Identifier.toIdentifier("")));
    Table table = new Table("orm", namespace, Identifier.toIdentifier(tableName), false);
    foreignKey.setTable(table);
    foreignKey.setReferencedTable(table);

    return foreignKey;
  }
}
