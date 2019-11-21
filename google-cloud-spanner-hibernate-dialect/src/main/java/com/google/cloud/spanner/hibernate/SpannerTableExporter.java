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

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * The exporter for Cloud Spanner CREATE and DROP table statements.
 *
 * @author Chengyuan Zhao
 */
public class SpannerTableExporter implements Exporter<Table> {

  private final SpannerTableStatements spannerTableStatements;

  private Map<Table, Table> tableDependencies;

  private HashSet<Table> processedTables;

  /**
   * Constructor.
   *
   * @param spannerDialect a Cloud Spanner dialect.
   */
  public SpannerTableExporter(SpannerDialect spannerDialect) {
    this.spannerTableStatements = new SpannerTableStatements(spannerDialect);
    this.tableDependencies = new HashMap<>();
    this.processedTables = new HashSet<>();
  }

  @Override
  public String[] getSqlCreateStrings(Table currentTable, Metadata metadata) {
    ArrayDeque<Table> tablesToProcess = getDependentTables(currentTable);
    for (Table table : tablesToProcess) {
      processedTables.add(table);
    }

    List<String> createTableStatements = tablesToProcess.stream()
        .flatMap(table -> spannerTableStatements.createTable(table, metadata).stream())
        .collect(Collectors.toList());
    return createTableStatements.toArray(new String[createTableStatements.size()]);
  }

  @Override
  public String[] getSqlDropStrings(Table currentTable, Metadata metadata) {
    ArrayDeque<Table> tablesToProcess = getDependentTables(currentTable);
    for (Table table : tablesToProcess) {
      processedTables.add(table);
    }

    List<String> dropTableStatements = tablesToProcess.stream()
        .flatMap(table -> spannerTableStatements.dropTable(table, metadata).stream())
        .collect(Collectors.toList());
    return dropTableStatements.toArray(new String[dropTableStatements.size()]);
  }

  /**
   * Initializes the table exporter's dependent tables.
   */
  public void initializeDependencies(Map<Table, Table> tableDependencies) {
    this.tableDependencies = tableDependencies;
    this.processedTables = new HashSet<>();
  }

  private ArrayDeque<Table> getDependentTables(Table table) {
    ArrayDeque<Table> tableStack = new ArrayDeque<>();
    while (table != null && !processedTables.contains(table)) {
      tableStack.push(table);
      table = tableDependencies.get(table);
    }

    return tableStack;
  }
}
