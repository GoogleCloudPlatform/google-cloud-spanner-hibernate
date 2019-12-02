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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * The exporter for Cloud Spanner CREATE and DROP table statements.
 *
 * @author Chengyuan Zhao
 */
public class SpannerTableExporter implements Exporter<Table> {

  private final SpannerTableStatements spannerTableStatements;

  private final TableDependencyTracker tableDependencyTracker;

  /**
   * Constructor.
   *
   * @param spannerDialect a Cloud Spanner dialect.
   */
  public SpannerTableExporter(SpannerDialect spannerDialect) {
    this.spannerTableStatements = new SpannerTableStatements(spannerDialect);
    this.tableDependencyTracker = new TableDependencyTracker();
  }

  @Override
  public String[] getSqlCreateStrings(Table currentTable, Metadata metadata) {
    initializeUniqueConstraints(currentTable);
    return buildSqlStrings(currentTable, metadata, true);
  }

  @Override
  public String[] getSqlDropStrings(Table currentTable, Metadata metadata) {
    initializeUniqueConstraints(currentTable);
    return buildSqlStrings(currentTable, metadata, false);
  }

  /**
   * Initializes the table exporter for if a new create-table or drop-table sequence is starting.
   */
  public void initializeTableExporter(Metadata metadata, boolean isCreateTables) {
    tableDependencyTracker.initializeDependencies(metadata, isCreateTables);
  }

  private String[] buildSqlStrings(Table currentTable, Metadata metadata, boolean isCreateTables) {
    Collection<Table> tablesToProcess = tableDependencyTracker.getDependentTables(currentTable);

    List<String> ddlStatements = tablesToProcess.stream()
        .flatMap(table -> {
          if (isCreateTables) {
            return spannerTableStatements.createTable(table, metadata).stream();
          } else {
            return spannerTableStatements.dropTable(table, metadata).stream();
          }
        })
        .collect(Collectors.toList());

    return ddlStatements.toArray(new String[ddlStatements.size()]);
  }

  /**
   * Processes the columns of the table and creates Unique Constraints for columns
   * annotated with @Column(unique = true).
   */
  private static void initializeUniqueConstraints(Table table) {
    Iterator<Column> colIterator = table.getColumnIterator();
    while (colIterator.hasNext()) {
      Column col = colIterator.next();
      if (col.isUnique()) {
        String name = Constraint.generateName("UK_", table, col);
        UniqueKey uk = table.getOrCreateUniqueKey(name);
        uk.addColumn(col);
      }
    }
  }
}
