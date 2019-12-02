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

package com.google.cloud.spanner.hibernate.schema;

import com.google.cloud.spanner.hibernate.Interleaved;
import com.google.cloud.spanner.hibernate.SpannerDialect;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hibernate.boot.Metadata;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;

/**
 * Generates the SQL statements for creating and dropping tables in Spanner.
 */
public class SpannerTableStatements {

  private static final String CREATE_TABLE_TEMPLATE =
      "create table {0} ({1}) PRIMARY KEY ({2}){3}";

  private final SpannerDialect spannerDialect;

  public SpannerTableStatements(SpannerDialect spannerDialect) {
    this.spannerDialect = spannerDialect;
  }

  /**
   * Generates the statements needed to drop a table.
   */
  public List<String> dropTable(Table table, Metadata metadata) {
    ArrayList<String> dropStrings = new ArrayList<>();

    Iterator<Index> iteratorIdx = table.getIndexIterator();
    while (iteratorIdx.hasNext()) {
      Index curr = iteratorIdx.next();
      dropStrings.add("drop index " + curr.getName());
    }

    dropStrings.add(this.spannerDialect.getDropTableString(table.getQuotedName()));
    return dropStrings;
  }

  /**
   * Generates the statements needed to create a table.
   */
  public List<String> createTable(Table table, Metadata metadata) {

    Iterable<Column> keyColumns;

    if (table.hasPrimaryKey()) {
      // a typical table that corresponds to an entity type
      keyColumns = getSortedPkColumns(table, metadata);
    } else if (isElementCollection(table, metadata)) {
      // a table that is actually an element collection property
      keyColumns = table::getColumnIterator;
    } else {
      // the case corresponding to a sequence-table that will only have 1 row.
      keyColumns = Collections.emptyList();
    }

    return getCreateTableStrings(table, metadata, keyColumns);
  }

  /**
   * Returns true if a table is generated by a Hibernate element collection.
   */
  private boolean isElementCollection(Table table, Metadata metadata) {
    for (Collection collection : metadata.getCollectionBindings()) {
      if (collection.getCollectionTable().equals(table)) {
        return true;
      }
    }
    return false;
  }

  private List<String> getCreateTableStrings(
      Table table, Metadata metadata, Iterable<Column> keyColumns) {

    // Get the comma separated string of the primary keys of the table.
    String primaryKeyColNames = StreamSupport.stream(keyColumns.spliterator(), false)
        .map(Column::getQuotedName)
        .collect(Collectors.joining(","));

    // Get the comma separated string of all columns of the table.
    Iterable<Column> columnIterable = () -> (Iterator<Column>) table.getColumnIterator();
    String allColumnNames = StreamSupport.stream(columnIterable.spliterator(), false)
        .map(column -> buildColumnTypeString(column, metadata))
        .collect(Collectors.joining(","));

    ArrayList<String> statements = new ArrayList<>();

    // Build the Create Table string.
    String createTableString = MessageFormat.format(
        CREATE_TABLE_TEMPLATE,
        table.getQuotedName(),
        allColumnNames,
        primaryKeyColNames,
        getInterleavedClause(table, metadata));

    statements.add(createTableString);

    // Hibernate requires the special hibernate_sequence table to be populated with an initial val.
    if (table.getName().equals(SequenceStyleGenerator.DEF_SEQUENCE_NAME)) {
      statements.add("INSERT INTO " + SequenceStyleGenerator.DEF_SEQUENCE_NAME + " ("
          + SequenceStyleGenerator.DEF_VALUE_COLUMN + ") VALUES(1)");
    }

    return statements;
  }

  /**
   * Converts a {@link Column} into its column + type string; i.e. "col_name string not null"
   */
  private String buildColumnTypeString(Column col, Metadata metadata) {
    return col.getQuotedName() + " " + col.getSqlType(this.spannerDialect, metadata)
        + (col.isNullable() ? this.spannerDialect.getNullColumnString() : " not null");
  }

  private static String getInterleavedClause(Table table, Metadata metadata) {
    Interleaved interleaved = SchemaUtils.getInterleaveAnnotation(table, metadata);
    if (interleaved != null) {
      Table parentTable = SchemaUtils.getTable(interleaved.parentEntity(), metadata);
      String interleaveClause = ", INTERLEAVE IN PARENT " + parentTable.getQuotedName();
      if (interleaved.cascadeDelete()) {
        interleaveClause += " ON DELETE CASCADE";
      }
      return interleaveClause;
    }

    return "";
  }

  private static List<Column> getSortedPkColumns(Table table, Metadata metadata) {
    Interleaved interleaved = SchemaUtils.getInterleaveAnnotation(table, metadata);
    if (interleaved == null) {
      return table.getPrimaryKey().getColumns();
    }

    Table parentTable = SchemaUtils.getTable(interleaved.parentEntity(), metadata);

    List<Column> sortedParentPkColumns = getSortedPkColumns(parentTable, metadata);
    List<Column> sortedCurrentPkColumns = table.getPrimaryKey().getColumns().stream()
        .filter(column -> !sortedParentPkColumns.contains(column))
        .collect(Collectors.toList());

    ArrayList<Column> currentPkColumns = new ArrayList<>();
    currentPkColumns.addAll(sortedParentPkColumns);
    currentPkColumns.addAll(sortedCurrentPkColumns);
    return currentPkColumns;
  }
}
