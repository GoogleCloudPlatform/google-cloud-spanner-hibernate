/*
 * Copyright 2019-2020 Google LLC
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

import com.google.cloud.spanner.Type.Code;
import com.google.cloud.spanner.hibernate.BitReversedSequenceStyleGenerator.ReplaceInitCommand;
import com.google.cloud.spanner.hibernate.Interleaved;
import com.google.cloud.spanner.hibernate.SpannerDialect;
import com.google.cloud.spanner.hibernate.types.AbstractSpannerArrayType;
import com.google.cloud.spanner.hibernate.types.SpannerArrayListType;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.boot.Metadata;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.type.CustomType;

/** Generates the SQL statements for creating and dropping tables in Spanner. */
public class SpannerTableStatements {

  private static final String CREATE_TABLE_TEMPLATE = "create table {0} ({1}) PRIMARY KEY ({2}){3}";

  private final SpannerDialect spannerDialect;

  private SpannerDatabaseInfo spannerDatabaseInfo;

  public SpannerTableStatements(SpannerDialect spannerDialect) {
    this.spannerDialect = spannerDialect;
  }

  /**
   * Initializes the {@link SpannerDatabaseInfo} which contains information about what tables and
   * indices exist in the Spanner database.
   *
   * @param spannerDatabaseInfo the {@link SpannerDatabaseInfo} to load.
   */
  public void initializeSpannerDatabaseInfo(SpannerDatabaseInfo spannerDatabaseInfo) {
    this.spannerDatabaseInfo = spannerDatabaseInfo;
  }

  /** Generates the statements needed to drop a table. */
  public List<String> dropTable(Table table) {
    ArrayList<String> dropStrings = new ArrayList<>();

    Set<String> existingTableIndices = spannerDatabaseInfo.getAllIndices().get(table);
    if (existingTableIndices != null) {
      for (String indexName : getTableIndices(table)) {
        if (existingTableIndices.contains(indexName)) {
          dropStrings.add("drop index if exists " + getQualifiedIndexName(table, indexName));
        }
      }
    }

    if (spannerDatabaseInfo.getAllTables().contains(table)) {
      // Drop all incoming foreign key constraints.
      Set<ForeignKey> exportedForeignKeys = spannerDatabaseInfo.getExportedForeignKeys(table);
      for (ForeignKey foreignKey : exportedForeignKeys) {
        if (foreignKey.getName() != null) {
          dropStrings.add(
              "alter table " + foreignKey.getTable().getQualifiedTableName().quote().getObjectName()
                  .toString()
                  + " drop constraint " + foreignKey.getName());
        }
      }

      dropStrings.add(this.spannerDialect.getDropTableString(
          table.getQualifiedTableName().quote().getObjectName().toString()));
    }
    return dropStrings;
  }

  private String getQualifiedIndexName(Table table, String index) {
    if (Strings.isNullOrEmpty(table.getSchema())) {
      return index;
    }
    return table.getSchema() + "." + index;
  }

  private Set<String> getTableIndices(Table table) {
    return Sets.union(table.getIndexes().keySet(), table.getUniqueKeys().keySet());
  }

  /** Generates the statements needed to create a table. */
  public List<String> createTable(Table table, Metadata metadata) {
    if (spannerDatabaseInfo.getAllTables().contains(table)) {
      return Collections.emptyList();
    }

    java.util.Collection<Column> keyColumns;

    if (table.hasPrimaryKey()) {
      // a typical table that corresponds to an entity type
      keyColumns = getSortedPkColumns(table, metadata);
    } else if (isElementCollection(table, metadata)) {
      // a table that is actually an element collection property
      keyColumns = table.getColumns();
    } else {
      // the case corresponding to a sequence-table that will only have 1 row.
      keyColumns = Collections.emptyList();
    }

    return getCreateTableStrings(table, metadata, keyColumns);
  }

  /** Returns true if a table is generated by a Hibernate element collection. */
  private boolean isElementCollection(Table table, Metadata metadata) {
    for (Collection collection : metadata.getCollectionBindings()) {
      if (collection.getCollectionTable().equals(table)) {
        return true;
      }
    }
    return false;
  }

  private List<String> getCreateTableStrings(
      Table table, Metadata metadata, java.util.Collection<Column> keyColumns) {

    // Get the comma separated string of the primary keys of the table.
    String primaryKeyColNames =
        keyColumns.stream()
            .map(Column::getQuotedName)
            .collect(Collectors.joining(","));

    // Get the comma separated string of all columns of the table.
    String allColumnNames =
        table.getColumns().stream()
            .map(column -> buildColumnTypeString(column, metadata))
            .collect(Collectors.joining(","));

    ArrayList<String> statements = new ArrayList<>();

    // Build the Create Table string.
    String createTableString =
        MessageFormat.format(
            CREATE_TABLE_TEMPLATE,
            table.getQualifiedTableName(),
            allColumnNames,
            primaryKeyColNames,
            getInterleavedClause(table, metadata));

    statements.add(createTableString);

    if (table.getName().equals(SequenceStyleGenerator.SEQUENCE_PARAM)) {
      // Caches the INSERT statement since DML statements must be run after a DDL batch.
      table.addInitCommand(
          context ->
              new ReplaceInitCommand(
                  "INSERT INTO "
                      + context.format(table.getQualifiedTableName())
                      + " ("
                      + SequenceStyleGenerator.DEF_VALUE_COLUMN
                      + ") VALUES(1)"));
    }

    return statements;
  }

  /** Converts a {@link Column} into its column + type string; i.e. "col_name string not null" */
  private String buildColumnTypeString(Column col, Metadata metadata) {
    String typeString;
    if (col.getValue() != null && col.getSqlTypeCode(metadata) == Types.ARRAY) {
      Code typeCode = Code.UNRECOGNIZED;
      if (col.getValue().getType() instanceof SpannerArrayListType) {
        typeCode = ((SpannerArrayListType) (col.getValue().getType())).getSpannerSqlType();
      } else {
        if (col.getValue().getType() instanceof CustomType) {
          CustomType<?> customType = (CustomType<?>) col.getValue().getType();
          if (customType.getUserType() instanceof AbstractSpannerArrayType) {
            typeCode =
                ((AbstractSpannerArrayType<?, ?>) customType.getUserType()).getSpannerTypeCode();
          }
        }
      }
      if (typeCode == Code.UNRECOGNIZED) {
        throw new IllegalArgumentException("Column " + col.getName()
            + " has type 'ARRAY', but the mapped Hibernate type is not a subclass of "
            + AbstractSpannerArrayType.class.getName());
      }

      String arrayType = typeCode.toString();
      if (typeCode == Code.STRING || typeCode == Code.BYTES) {
        // If String or Bytes, must specify size in parentheses.
        if (col.getLength() == null) {
          arrayType += "(MAX)";
        } else {
          arrayType += "(" + col.getLength() + ")";
        }
      }
      typeString = String.format("ARRAY<%s>", arrayType);
    } else {
      typeString = col.getSqlType(metadata);
    }

    String result = col.getQuotedName() + " " + typeString
        + (col.isNullable() ? this.spannerDialect.getNullColumnString() : " not null");
    if (col.getDefaultValue() != null) {
      result = result + " default (" + col.getDefaultValue() + ")";
    }
    
    return result;
  }

  private static String getInterleavedClause(Table table, Metadata metadata) {
    Interleaved interleaved = SchemaUtils.getInterleaveAnnotation(table, metadata);
    if (interleaved != null) {
      Table parentTable = SchemaUtils.getTable(interleaved.parentEntity(), metadata);
      String interleaveClause = ", INTERLEAVE IN PARENT " + parentTable.getQualifiedTableName();
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
    List<Column> sortedCurrentPkColumns =
        table.getPrimaryKey().getColumns().stream()
            .filter(column -> !sortedParentPkColumns.contains(column))
            .collect(Collectors.toList());

    ArrayList<Column> currentPkColumns = new ArrayList<>();
    currentPkColumns.addAll(sortedParentPkColumns);
    currentPkColumns.addAll(sortedCurrentPkColumns);
    return currentPkColumns;
  }
}