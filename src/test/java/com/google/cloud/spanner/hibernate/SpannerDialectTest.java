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

import com.google.cloud.spanner.hibernate.SpannerDialect.DoNothingLockingStrategy;
import org.hibernate.LockOptions;
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

  /* Lock acquisition functions */
  @Test
  public void supportsLockTimeoutsTest() {
    assertFalse(this.spannerDialect.supportsLockTimeouts());
  }

  @Test
  public void getLockingStrategyTest() {
    assertTrue(
        this.spannerDialect.getLockingStrategy(null, null) instanceof DoNothingLockingStrategy);
  }

  @Test
  public void getForUpdateStringLockOptionsTest() {
    assertEquals("", this.spannerDialect.getForUpdateString((LockOptions) null));
  }

  @Test
  public void getForUpdateStringTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getForUpdateString();
  }

  @Test
  public void getWriteLockStringTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getWriteLockString(1);
  }

  @Test
  public void getWriteLockStringAliasTimeoutTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getWriteLockString("a", 1);
  }

  @Test
  public void getReadLockStringTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getReadLockString(1);
  }

  @Test
  public void getReadLockStringAliasTimeoutTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getReadLockString("a", 1);
  }

  @Test
  public void supportsOuterJoinForUpdateTest() {
    assertFalse(this.spannerDialect.supportsOuterJoinForUpdate());
  }

  @Test
  public void isLockTimeoutParameterizedTest() {
    assertFalse(this.spannerDialect.isLockTimeoutParameterized());
  }

  @Test
  public void forUpdateOfColumnsTest() {
    assertFalse(this.spannerDialect.forUpdateOfColumns());
  }

  @Test
  public void getForUpdateStringAliasTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getForUpdateString("a");
  }

  @Test
  public void getForUpdateStringAliasLockOptionsTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getForUpdateString("a", null);
  }

  @Test
  public void getForUpdateNowaitStringTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getForUpdateNowaitString();
  }

  @Test
  public void getForUpdateSkipLockedStringTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getForUpdateSkipLockedString();
  }

  @Test
  public void getForUpdateNowaitStringAliasTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getForUpdateNowaitString("a");
  }

  @Test
  public void getForUpdateSkipLockedStringAliasTest() {
    this.expectedException.expect(UnsupportedOperationException.class);
    this.spannerDialect.getForUpdateSkipLockedString("a");
  }

  @Test
  public void appendLockHintTest() {
    assertEquals("original_table_name",
        this.spannerDialect.appendLockHint((LockOptions) null, "original_table_name"));
  }

  @Test
  public void applyLocksToSqlTest() {
    assertEquals("original statement",
        this.spannerDialect.applyLocksToSql("original statement", null, null));
  }
}
