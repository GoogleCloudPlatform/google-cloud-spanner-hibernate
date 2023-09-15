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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.spanner.hibernate.schema.SpannerDatabaseInfo;
import com.google.cloud.spanner.hibernate.schema.SpannerTableStatements;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests to verify DDL statement generation for table creation.
 */
public class SpannerTableStatementsTests {

  private SpannerTableStatements spannerTableStatements;

  private Metadata metadata;

  /**
   * Setup the mocks for the test.
   */
  @Before
  public void setupSpannerDatabaseInfo() {
    // Metadata mocks
    metadata = Mockito.mock(Metadata.class);
    Database database = mock(Database.class);
    when(metadata.getDatabase()).thenReturn(database);
    TypeConfiguration typeConfiguration = mock(TypeConfiguration.class);
    when(database.getTypeConfiguration()).thenReturn(typeConfiguration);

    // Initialize SpannerDatabaseInfo
    SpannerDatabaseInfo spannerDatabaseInfo = Mockito.mock(SpannerDatabaseInfo.class);
    when(spannerDatabaseInfo.getAllTables()).thenReturn(
        new HashSet<>(Arrays.asList("Student", "Teacher", "House")));
    when(spannerDatabaseInfo.getAllIndices()).thenReturn(
        new HashSet<>(Collections.singletonList("address")));

    // Initialize SpannerStatements
    spannerTableStatements = new SpannerTableStatements(new SpannerDialect());
    spannerTableStatements.initializeSpannerDatabaseInfo(spannerDatabaseInfo);
  }

  @Test
  public void testDropTableStatement() {
    Table table = new Table("orm");
    table.setName("House");

    List<String> statements = spannerTableStatements.dropTable(table);
    assertThat(statements).containsExactly("drop table House");
  }

  @Test
  public void testDropTableStatement_missingTable() {
    Table table = new Table("orm");
    table.setName("Missing_Table");

    List<String> statements = spannerTableStatements.dropTable(table);
    assertThat(statements).isEmpty();
  }

  @Test
  public void testDropTableStatement_withIndex() {
    Table table = new Table("orm");
    table.setName("House");

    Index index = new Index();
    index.setName("address");
    table.addIndex(index);

    List<String> statements = spannerTableStatements.dropTable(table);
    assertThat(statements).containsExactly("drop index address", "drop table House");
  }

  @Test
  public void testCreateTableStatement() {
    Table table = new Table("orm");
    table.setName("Test");

    Column col = new Column();
    col.setName("id");
    col.setSqlType("INT64");

    PrimaryKey primaryKey = new PrimaryKey(table);
    primaryKey.setName("pk");
    primaryKey.addColumn(col);
    table.setPrimaryKey(primaryKey);

    col = new Column();
    col.setName("name");
    col.setSqlType("STRING(255)");
    table.addColumn(col);

    List<String> statements = spannerTableStatements.createTable(table, metadata);
    assertThat(statements).containsExactly("create table Test (name STRING(255)) PRIMARY KEY (id)");
  }
}
