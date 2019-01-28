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

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * Hibernate Dialect implementation for Cloud Spanner.
 *
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 */
public class SpannerDialect extends Dialect {

  private final SpannerTableExporter spannerTableExporter = new SpannerTableExporter(this);

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
}
