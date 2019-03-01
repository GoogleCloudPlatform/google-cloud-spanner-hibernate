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

import java.io.Serializable;
import java.util.Map;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.StaleObjectStateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.LockingStrategyException;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * Hibernate Dialect implementation for Cloud Spanner.
 *
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 * @author Daniel Zou
 */
public class SpannerDialect extends Dialect {

  private final SpannerTableExporter spannerTableExporter = new SpannerTableExporter(this);

  private static final LockingStrategy LOCKING_STRATEGY = new DoNothingLockingStrategy();

  private static final Exporter NOOP_EXPORTER = new EmptyExporter();

  private static final UniqueDelegate NOOP_UNIQUE_DELEGATE = new DoNothingUniqueDelegate();

  @Override
  public Exporter<Table> getTableExporter() {
    return this.spannerTableExporter;
  }

  /* SELECT-related functions */

  @Override
  public boolean supportsCurrentTimestampSelection() {
    return true;
  }

  @Override
  public boolean isCurrentTimestampSelectStringCallable() {
    return false;
  }

  @Override
  public String getCurrentTimestampSelectString() {
    return "SELECT CURRENT_TIMESTAMP() as now";
  }

  @Override
  public String toBooleanValueString(boolean bool) {
    return bool ? "TRUE" : "FALSE";
  }

  @Override
  public boolean supportsUnionAll() {
    return true;
  }

  @Override
  public boolean supportsCaseInsensitiveLike() {
    return false;
  }

  /* DDL-related functions */

  @Override
  public boolean canCreateSchema() {
    return false;
  }

  @Override
  public String[] getCreateSchemaCommand(String schemaName) {
    throw new UnsupportedOperationException(
        "No create schema syntax supported by " + getClass().getName());
  }

  @Override
  public String[] getDropSchemaCommand(String schemaName) {
    throw new UnsupportedOperationException(
        "No drop schema syntax supported by " + getClass().getName());
  }

  @Override
  public String getCurrentSchemaCommand() {
    throw new UnsupportedOperationException("No current schema syntax supported by "
        + getClass().getName());
  }

  @Override
  public SchemaNameResolver getSchemaNameResolver() {
    throw new UnsupportedOperationException(
        "No schema name resolver supported by " + getClass().getName());
  }

  @Override
  public boolean dropConstraints() {
    return false;
  }

  @Override
  public boolean qualifyIndexName() {
    return false;
  }

  @Override
  public String getAddColumnString() {
    return "ADD COLUMN";
  }

  @Override
  public String getDropForeignKeyString() {
    throw new UnsupportedOperationException("Cannot drop foreign-key constraint because "
        + "Cloud Spanner does not support foreign keys.");
  }

  @Override
  public String getAddForeignKeyConstraintString(String constraintName,
      String[] foreignKey,
      String referencedTable,
      String[] primaryKey,
      boolean referencesPrimaryKey) {
    throw new UnsupportedOperationException("Cannot add foreign-key constraint because "
        + "Cloud Spanner does not support foreign keys.");
  }

  @Override
  public String getAddForeignKeyConstraintString(
      String constraintName,
      String foreignKeyDefinition) {
    throw new UnsupportedOperationException("Cannot add foreign-key constraint because "
        + "Cloud Spanner does not support foreign keys.");
  }

  @Override
  public String getAddPrimaryKeyConstraintString(String constraintName) {
    throw new UnsupportedOperationException("Cannot add primary key constraint in Cloud Spanner.");
  }

  /* Lock acquisition functions */

  @Override
  public boolean supportsLockTimeouts() {
    return false;
  }

  @Override
  public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
    return LOCKING_STRATEGY;
  }

  @Override
  public String getForUpdateString(LockOptions lockOptions) {
    return "";
  }

  @Override
  public String getForUpdateString() {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }

  @Override
  public String getForUpdateString(String aliases) {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }

  @Override
  public String getForUpdateString(String aliases, LockOptions lockOptions) {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }

  @Override
  public String getWriteLockString(int timeout) {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }

  @Override
  public String getWriteLockString(String aliases, int timeout) {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }

  @Override
  public String getReadLockString(int timeout) {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }

  @Override
  public String getReadLockString(String aliases, int timeout) {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }

  @Override
  public boolean supportsOuterJoinForUpdate() {
    return false;
  }

  @Override
  public String getForUpdateNowaitString() {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }

  @Override
  public String getForUpdateNowaitString(String aliases) {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }


  @Override
  public String getForUpdateSkipLockedString() {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }

  @Override
  public String getForUpdateSkipLockedString(String aliases) {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }

  /* Unsupported Hibernate Exporters */

  @Override
  public Exporter<Sequence> getSequenceExporter() {
    return NOOP_EXPORTER;
  }

  @Override
  public Exporter<ForeignKey> getForeignKeyExporter() {
    return NOOP_EXPORTER;
  }

  @Override
  public Exporter<Constraint> getUniqueKeyExporter() {
    return NOOP_EXPORTER;
  }

  @Override
  public String applyLocksToSql(String sql, LockOptions aliasedLockOptions,
      Map<String, String[]> keyColumnNames) {
    return sql;
  }

  @Override
  public UniqueDelegate getUniqueDelegate() {
    return NOOP_UNIQUE_DELEGATE;
  }

  /**
   * The Cloud Spanner Hibernate Dialect does not currently support UNIQUE restrictions.
   *
   * @return {@code false}.
   */
  @Override
  public boolean supportsUnique() {
    return false;
  }

  /**
   * The Cloud Spanner Hibernate Dialect does not currently support UNIQUE restrictions.
   *
   * @return {@code false}.
   */
  @Override
  public boolean supportsNotNullUnique() {
    return false;
  }

  /**
   * The Cloud Spanner Hibernate Dialect does not currently support UNIQUE restrictions.
   *
   * @return {@code false}.
   */
  @Override
  public boolean supportsUniqueConstraintInCreateAlterTable() {
    return false;
  }

  @Override
  public String getAddUniqueConstraintString(String constraintName) {
    return "";
  }

  @Override
  public boolean supportsCircularCascadeDeleteConstraints() {
    return false;
  }

  @Override
  public boolean supportsCascadeDelete() {
    return false;
  }

  @Override
  public char openQuote() {
    return '`';
  }

  @Override
  public char closeQuote() {
    return '`';
  }

  /**
   * A locking strategy for the Cloud Spanner dialect that does nothing. Cloud Spanner does not
   * support locking.
   *
   * @author Chengyuan Zhao
   */
  static class DoNothingLockingStrategy implements LockingStrategy {

    @Override
    public void lock(Serializable id, Object version, Object object, int timeout,
        SharedSessionContractImplementor session)
        throws StaleObjectStateException, LockingStrategyException {
      // Do nothing. Cloud Spanner doesn't have have locking strategies.
    }
  }

  /**
   * A no-op {@link Exporter} which is responsible for returning empty Create and Drop SQL strings.
   *
   * @author Daniel Zou
   */
  static class EmptyExporter<T extends Exportable> implements Exporter<T> {

    @Override
    public String[] getSqlCreateStrings(T exportable, Metadata metadata) {
      return new String[0];
    }

    @Override
    public String[] getSqlDropStrings(T exportable, Metadata metadata) {
      return new String[0];
    }
  }

  /**
   * A no-op delegate for generating Unique-Constraints. Cloud Spanner offers unique-restrictions
   * via interleaved indexes with the "UNIQUE" option. This is not currently supported.
   *
   * @author Chengyuan Zhao
   */
  static class DoNothingUniqueDelegate implements UniqueDelegate {

    @Override
    public String getColumnDefinitionUniquenessFragment(Column column) {
      return "";
    }

    @Override
    public String getTableCreationUniqueConstraintsFragment(Table table) {
      return "";
    }

    @Override
    public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata) {
      return "";
    }

    @Override
    public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata) {
      return "";
    }
  }
}
