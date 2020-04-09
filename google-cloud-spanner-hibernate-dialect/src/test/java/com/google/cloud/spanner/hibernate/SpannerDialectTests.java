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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.cloud.spanner.hibernate.SpannerDialect.DoNothingLockingStrategy;
import org.hibernate.LockOptions;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for SpannerDialect.
 *
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 */
public class SpannerDialectTests {

  private SpannerDialect spannerDialect;

  @Before
  public void setUp() {
    this.spannerDialect = new SpannerDialect();
  }

  @Test
  public void dropTableStringTest() {
    String dropTableString = this.spannerDialect.getDropTableString("test_table");
    assertThat(dropTableString).isEqualTo("drop table test_table");
  }

  @Test
  public void getTableExporterTest() {
    assertThat(this.spannerDialect.getTableExporter()).isNotNull();
  }

  /* DDL-related function tests */

  @Test
  public void canCreateCatalogTest() {
    assertThat(this.spannerDialect.canCreateCatalog()).isFalse();
  }

  @Test
  public void createCatalogStatementTest() {
    assertThatThrownBy(() -> this.spannerDialect.getCreateCatalogCommand("test"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void createDropCatalogStatementTest() {
    assertThatThrownBy(() -> this.spannerDialect.getDropCatalogCommand("test"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void canCreateSchemaTest() {
    assertThat(this.spannerDialect.canCreateSchema()).isFalse();
  }

  @Test
  public void createSchemaStatementTest() {
    assertThatThrownBy(() -> this.spannerDialect.getCreateSchemaCommand("test"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void dropCatalogStatementTest() {
    assertThatThrownBy(() -> this.spannerDialect.getDropSchemaCommand("test"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void getCurrentSchemaTest() {
    assertThatThrownBy(() -> this.spannerDialect.getCurrentSchemaCommand())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void getSchemaResolverTest() {
    assertThatThrownBy(() -> this.spannerDialect.getSchemaNameResolver())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void hasAlterTableTest() {
    assertThat(this.spannerDialect.hasAlterTable()).isTrue();
  }

  @Test
  public void dropConstraintsTest() {
    assertThat(this.spannerDialect.dropConstraints()).isFalse();
  }

  @Test
  public void qualifyIndexNameTest() {
    assertThat(this.spannerDialect.qualifyIndexName()).isFalse();
  }

  @Test
  public void getAddColumnStringTest() {
    assertThat(this.spannerDialect.getAddColumnString()).isEqualTo("ADD COLUMN");
  }

  @Test
  public void getAddColumnSuffixText() {
    assertThat(this.spannerDialect.getAddColumnSuffixString()).isEqualTo("");
  }

  @Test
  public void getTableTypeString() {
    assertThat(this.spannerDialect.getTableTypeString()).isEqualTo("");
  }

  @Test
  public void getAddPrimaryKeyConstraintStringTest() {
    assertThatThrownBy(() -> this.spannerDialect.getAddPrimaryKeyConstraintString(null))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void hasSelfReferentialForeignKeyBugTest() {
    assertThat(this.spannerDialect.hasSelfReferentialForeignKeyBug()).isFalse();
  }

  @Test
  public void getNullColumnStringTest() {
    assertThat(this.spannerDialect.getNullColumnString()).isEqualTo("");
  }

  @Test
  public void supportsCommentOnTest() {
    assertThat(this.spannerDialect.supportsCommentOn()).isFalse();
  }

  @Test
  public void getTableCommentTEst() {
    assertThat(this.spannerDialect.getTableComment("test")).isEqualTo("");
  }

  @Test
  public void getColumnCommentTEst() {
    assertThat(this.spannerDialect.getColumnComment("test")).isEqualTo("");
  }

  @Test
  public void supportsIfExistsBeforeTableNameTest() {
    assertThat(this.spannerDialect.supportsIfExistsBeforeTableName()).isFalse();
  }

  @Test
  public void supportsIfExistsAfterTableNameTest() {
    assertThat(this.spannerDialect.supportsIfExistsAfterTableName()).isFalse();
  }

  @Test
  public void supportsIfExistsBeforeConstraintNameTest() {
    assertThat(this.spannerDialect.supportsIfExistsBeforeConstraintName()).isFalse();
  }

  @Test
  public void supportsIfExistsAfterConstraintNameTest() {
    assertThat(this.spannerDialect.supportsIfExistsAfterConstraintName()).isFalse();
  }

  @Test
  public void supportsIfExistsAfterAlterTableTest() {
    assertThat(this.spannerDialect.supportsIfExistsAfterAlterTable()).isFalse();
  }

  @Test
  public void getDropStringTest() {
    String dropTableString = this.spannerDialect.getDropTableString("test_table");
    assertThat(dropTableString).isEqualTo("drop table test_table");
  }

  @Test
  public void getCreateTableStringTest() {
    assertThat(this.spannerDialect.getCreateTableString()).isEqualTo("create table");
  }

  /* Lock acquisition functions */
  @Test
  public void supportsLockTimeoutsTest() {
    assertThat(this.spannerDialect.supportsLockTimeouts()).isFalse();
  }

  @Test
  public void getLockingStrategyTest() {
    assertThat(this.spannerDialect.getLockingStrategy(null, null))
        .isInstanceOf(DoNothingLockingStrategy.class);
  }

  @Test
  public void getForUpdateStringLockOptionsTest() {
    assertThat(this.spannerDialect.getForUpdateString((LockOptions) null)).isEqualTo("");
  }

  @Test
  public void getForUpdateStringTest() {
    assertThat(this.spannerDialect.getForUpdateString((LockOptions) null)).isEqualTo("");
  }

  @Test
  public void getWriteLockStringTest() {
    assertThatThrownBy(() -> this.spannerDialect.getWriteLockString(1))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void getWriteLockStringAliasTimeoutTest() {
    assertThatThrownBy(() -> this.spannerDialect.getWriteLockString("a", 1))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void getReadLockStringTest() {
    assertThatThrownBy(() -> this.spannerDialect.getReadLockString(1))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void getReadLockStringAliasTimeoutTest() {
    assertThatThrownBy(() -> this.spannerDialect.getReadLockString("a", 1))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void supportsOuterJoinForUpdateTest() {
    assertThat(this.spannerDialect.supportsOuterJoinForUpdate()).isFalse();
  }

  @Test
  public void isLockTimeoutParameterizedTest() {
    assertThat(this.spannerDialect.isLockTimeoutParameterized()).isFalse();
  }

  @Test
  public void forUpdateOfColumnsTest() {
    assertThat(this.spannerDialect.forUpdateOfColumns()).isFalse();
  }

  @Test
  public void getForUpdateStringAliasTest() {
    assertThatThrownBy(() -> this.spannerDialect.getForUpdateString("a"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void getForUpdateStringAliasLockOptionsTest() {
    assertThatThrownBy(() -> this.spannerDialect.getForUpdateString("a", null))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void getForUpdateNowaitStringTest() {
    assertThatThrownBy(() -> this.spannerDialect.getForUpdateNowaitString())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void getForUpdateSkipLockedStringTest() {
    assertThatThrownBy(() -> this.spannerDialect.getForUpdateSkipLockedString())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void getForUpdateNowaitStringAliasTest() {
    assertThatThrownBy(() -> this.spannerDialect.getForUpdateNowaitString("a"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void getForUpdateSkipLockedStringAliasTest() {
    assertThatThrownBy(() -> this.spannerDialect.getForUpdateSkipLockedString("a"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void appendLockHintTest() {
    String lockHint = this.spannerDialect.appendLockHint((LockOptions) null, "original_table_name");
    assertThat(lockHint).isEqualTo("original_table_name");
  }

  @Test
  public void applyLocksToSqlTest() {
    String originalStatement = this.spannerDialect.applyLocksToSql(
        "original statement", null, null);
    assertThat(originalStatement).isEqualTo("original statement");
  }

  @Test
  public void supportsCascadeDeleteTest() {
    assertThat(this.spannerDialect.supportsCascadeDelete()).isFalse();
  }

  @Test
  public void supportsCircularCascadeDeleteConstraintsTest() {
    assertThat(this.spannerDialect.supportsCircularCascadeDeleteConstraints()).isFalse();
  }
}
