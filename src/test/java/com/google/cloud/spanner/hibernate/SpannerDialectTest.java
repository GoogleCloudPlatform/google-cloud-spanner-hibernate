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

package com.google.cloud.spanner.hibernate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit test for SpannerDialect.
 *
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 */
public class SpannerDialectTest {

  private SpannerDialect spannerDialect;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() {
    this.spannerDialect = new SpannerDialect();
  }

  @Test
  public void dropTableStringTest() {
    assertEquals("drop table test_table", this.spannerDialect.getDropTableString("test_table"));
  }

  @Test
  public void getTableExporterTest() {
    assertNotNull(this.spannerDialect.getTableExporter());
  }

  /* DDL-related function tests */

  @Test
  public void canCreateCatalogTest() {
    assertFalse(this.spannerDialect.canCreateCatalog());
  }

  @Test
  public void createCatalogStatementTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getCreateCatalogCommand("test");
  }

  @Test
  public void createDropCatalogStatementTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getDropCatalogCommand("test");
  }

  @Test
  public void canCreateSchemaTest() {
    assertFalse(this.spannerDialect.canCreateSchema());
  }

  @Test
  public void createSchemaStatementTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getCreateSchemaCommand("test");
  }

  @Test
  public void dropCatalogStatementTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getDropSchemaCommand("test");
  }

  @Test
  public void getCurrentSchemaTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getCurrentSchemaCommand();
  }

  @Test
  public void getSchemaResolverTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getSchemaNameResolver();
  }

  @Test
  public void hasAlterTableTest() {
    assertTrue(this.spannerDialect.hasAlterTable());
  }

  @Test
  public void dropConstraintsTest() {
    assertFalse(this.spannerDialect.dropConstraints());
  }

  @Test
  public void qualifyIndexNameTest() {
    assertFalse(this.spannerDialect.qualifyIndexName());
  }

  @Test
  public void getAddColumnStringTest() {
    assertEquals("ADD COLUMN", this.spannerDialect.getAddColumnString());
  }

  @Test
  public void getAddColumnSuffixText() {
    assertEquals("", this.spannerDialect.getAddColumnSuffixString());
  }

  @Test
  public void getDropKeyConstraintTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getDropForeignKeyString();
  }

  @Test
  public void getTableTypeString() {
    assertEquals("", this.spannerDialect.getTableTypeString());
  }

  @Test
  public void getAddForeignKeyConstraintStringTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getAddForeignKeyConstraintString(null, null, null, null, false);
  }

  @Test
  public void getAddForeignKeyConstraintStringSimpifiedTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getAddForeignKeyConstraintString(null, null);
  }

  @Test
  public void getAddPrimaryKeyConstraintStringTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getAddPrimaryKeyConstraintString(null);
  }

  @Test
  public void hasSelfReferentialForeignKeyBugTest() {
    assertFalse(this.spannerDialect.hasSelfReferentialForeignKeyBug());
  }

  @Test
  public void getNullColumnStringTest() {
    assertEquals("", this.spannerDialect.getNullColumnString());
  }

  @Test
  public void supportsCommentOnTest() {
    assertFalse(this.spannerDialect.supportsCommentOn());
  }

  @Test
  public void getTableCommentTEst() {
    assertEquals("", this.spannerDialect.getTableComment("test"));
  }

  @Test
  public void getColumnCommentTEst() {
    assertEquals("", this.spannerDialect.getColumnComment("test"));
  }

  @Test
  public void supportsIfExistsBeforeTableNameTest() {
    assertFalse(this.spannerDialect.supportsIfExistsBeforeTableName());
  }

  @Test
  public void supportsIfExistsAfterTableNameTest() {
    assertFalse(this.spannerDialect.supportsIfExistsAfterTableName());
  }

  @Test
  public void supportsIfExistsBeforeConstraintNameTest() {
    assertFalse(this.spannerDialect.supportsIfExistsBeforeConstraintName());
  }

  @Test
  public void supportsIfExistsAfterConstraintNameTest() {
    assertFalse(this.spannerDialect.supportsIfExistsAfterConstraintName());
  }

  @Test
  public void supportsIfExistsAfterAlterTableTest() {
    assertFalse(this.spannerDialect.supportsIfExistsAfterAlterTable());
  }

  @Test
  public void getDropStringTest() {
    assertEquals("drop table test_table", this.spannerDialect.getDropTableString("test_table"));
  }

  @Test
  public void getCreateTableStringTest() {
    assertEquals("create table", this.spannerDialect.getCreateTableString());
  }
}
