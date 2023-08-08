/*
 * Copyright 2023 Google LLC
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

import com.google.cloud.spanner.hibernate.schema.SpannerForeignKeyExporter;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.spi.Exporter;

/** Hibernate 6.x dialect for Cloud Spanner. */
public class SpannerDialect extends org.hibernate.dialect.SpannerDialect {
  private static class NoOpSqmMultiTableInsertStrategy implements SqmMultiTableInsertStrategy {
    private static final NoOpSqmMultiTableInsertStrategy INSTANCE =
        new NoOpSqmMultiTableInsertStrategy();

    @Override
    public int executeInsert(
        SqmInsertStatement<?> sqmInsertStatement,
        DomainParameterXref domainParameterXref,
        DomainQueryExecutionContext context) {
      throw new HibernateException("Multi-table inserts are not supported for Cloud Spanner");
    }
  }

  private final SpannerTableExporter spannerTableExporter = new SpannerTableExporter(this);

  private final SpannerForeignKeyExporter spannerForeignKeyExporter =
      new SpannerForeignKeyExporter(this);

  @Override
  public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
    return new StandardSqlAstTranslatorFactory() {
      @Override
      protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
          SessionFactoryImplementor sessionFactory, Statement statement) {
        return new SpannerSqlAstTranslator<>(sessionFactory, statement);
      }
    };
  }

  @Override
  public Exporter<Table> getTableExporter() {
    return this.spannerTableExporter;
  }

  @Override
  public Exporter<ForeignKey> getForeignKeyExporter() {
    return this.spannerForeignKeyExporter;
  }

  @Override
  public String getDropForeignKeyString() {
    // TODO: Remove when the override in the super class has been fixed.
    return "drop constraint";
  }

  @Override
  public String getAddForeignKeyConstraintString(
      String constraintName,
      String[] foreignKey,
      String referencedTable,
      String[] primaryKey,
      boolean referencesPrimaryKey) {
    // TODO: Remove when the override in the super class has been fixed.
    return " add constraint "
        + quote(constraintName)
        + " foreign key ("
        + String.join(", ", foreignKey)
        + ") references "
        + referencedTable
        // Cloud Spanner requires the referenced columns to specified in all cases, including
        // if the foreign key is referencing the primary key of the referenced table.
        + " ("
        + String.join(", ", primaryKey)
        + ')';
  }

  @Override
  public String getAddForeignKeyConstraintString(
      String constraintName, String foreignKeyDefinition) {
    // TODO: Remove when the override in the super class has been fixed.
    return " add constraint " + quote(constraintName) + " " + foreignKeyDefinition;
  }

  @Override
  public boolean supportsCascadeDelete() {
    return true;
  }

  @Override
  public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
      EntityMappingType entityDescriptor, RuntimeModelCreationContext runtimeModelCreationContext) {
    return NoOpSqmMultiTableInsertStrategy.INSTANCE;
  }
}
