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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.spanner.hibernate.schema.SpannerDatabaseInfo;
import com.google.cloud.spanner.hibernate.schema.SpannerForeignKeyExporter;
import java.util.Collections;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.junit.Before;
import org.junit.Test;

public class SpannerForeignKeyExporterTests {

  private SpannerForeignKeyExporter spannerForeignKeyExporter;

  private SpannerDatabaseInfo spannerDatabaseInfo;

  private Metadata metadata;

  /**
   * Setup the mocks needed for the Spanner foreign key tests.
   */
  @Before
  public void setup() {
    this.spannerDatabaseInfo = mock(SpannerDatabaseInfo.class);
    when(spannerDatabaseInfo.getAllTables()).thenReturn(Collections.singleton("address"));
    when(spannerDatabaseInfo.getImportedForeignKeys("address"))
        .thenReturn(Collections.singleton("address_fk"));

    this.metadata = setupMetadataMock();
    this.spannerForeignKeyExporter = new SpannerForeignKeyExporter(new SpannerDialect());
    this.spannerForeignKeyExporter.init(spannerDatabaseInfo);
  }

  @Test
  public void testDropForeignKey() {
    String[] dropStatements = spannerForeignKeyExporter.getSqlDropStrings(
        foreignKey("address", "address_fk"), metadata);
    assertThat(dropStatements).containsExactly("alter table address drop constraint address_fk");
  }

  @Test
  public void testDropMissingForeignKey_missingTable() {
    String[] dropStatements = spannerForeignKeyExporter.getSqlDropStrings(
        foreignKey("person", "person_fk"), metadata);
    assertThat(dropStatements).isEmpty();
  }

  @Test
  public void testDropMissingForeignKey_missingForeignKey() {
    String[] dropStatements = spannerForeignKeyExporter.getSqlDropStrings(
        foreignKey("address", "other_fk"), metadata);
    assertThat(dropStatements).isEmpty();
  }

  private static ForeignKey foreignKey(String tableName, String foreignKeyName) {
    ForeignKey foreignKey = new ForeignKey();
    foreignKey.setName(foreignKeyName);

    Table table = new Table();
    table.setName(tableName);
    foreignKey.setTable(table);
    foreignKey.setReferencedTable(table);

    return foreignKey;
  }

  private static Metadata setupMetadataMock() {
    Metadata metadata = mock(Metadata.class);
    Database database = mock(Database.class);
    JdbcEnvironment jdbcEnvironment = mock(JdbcEnvironment.class);
    QualifiedObjectNameFormatter qualifiedObjectNameFormatter =
        mock(QualifiedObjectNameFormatter.class);

    when(metadata.getDatabase()).thenReturn(database);
    when(database.getJdbcEnvironment()).thenReturn(jdbcEnvironment);
    when(jdbcEnvironment.getQualifiedObjectNameFormatter())
        .thenReturn(qualifiedObjectNameFormatter);
    when(qualifiedObjectNameFormatter.format(any(QualifiedTableName.class), any()))
        .thenAnswer(invocation ->
            ((QualifiedTableName) invocation.getArguments()[0]).getTableName().getCanonicalName());

    return metadata;
  }
}

