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
import java.util.Map;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Table;

/**
 * Tracks the order in which tables should be processed (created/dropped) by Hibernate.
 *
 * <p>In Spanner, interleaved tables must be created or dropped in the correct order. For example,
 * one must create a parent table before its interleaved table. And one must drop the interleaved
 * table first before its parent table.
 */
public class TableDependencyTracker {

  // For each map entry (key, value), the key is a table which is being blocked by the
  // table stored as the value.
  private Map<Table, Table> tableDependencies;

  private HashSet<Table> processedTables;

  /**
   * Initializes the table dependency tracker.
   *
   * @param metadata the Hibernate metadata
   * @param isCreateTable whether the initialization is occurring for creating tables
   */
  public void initializeDependencies(Metadata metadata, boolean isCreateTable) {
    HashMap<Table, Table> dependencies = new HashMap<>();

    for (Table childTable : metadata.collectTableMappings()) {
      Interleaved interleaved = SchemaUtils.getInterleaveAnnotation(childTable, metadata);
      if (interleaved != null) {
        if (isCreateTable) {
          // If creating tables, the parent blocks the child.
          dependencies.put(childTable, SchemaUtils.getTable(interleaved.parent(), metadata));
        } else {
          // If dropping tables, the child blocks the parent.
          dependencies.put(SchemaUtils.getTable(interleaved.parent(), metadata), childTable);
        }
      }
    }

    this.tableDependencies = dependencies;
    this.processedTables = new HashSet<>();
  }

  /**
   * Returns the list of tables that one must process before processing the provided {@code table}.
   *
   * @param table The table that you wish to process
   * @return the ordered {@link ArrayDeque} of tables to process before processing {@code table}
   */
  public ArrayDeque<Table> getDependentTables(Table table) {
    ArrayDeque<Table> tableStack = new ArrayDeque<>();
    while (table != null && !processedTables.contains(table)) {
      tableStack.push(table);
      processedTables.add(table);

      table = tableDependencies.get(table);
    }

    return tableStack;
  }
}
