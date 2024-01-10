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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.mapping.Table;

/**
 * Helper class for extracting information from the {@link DatabaseMetaData} which contains
 * information about what tables and indices currently exist in the database.
 */
public class SpannerDatabaseInfo {
  
  private final Set<Table> tableNames;

  private final Map<Table, Set<String>> indexNames;

  private final DatabaseMetaData databaseMetaData;

  /**
   * Constructs the {@link SpannerDatabaseInfo} by querying the Spanner database metadata.
   */
  public SpannerDatabaseInfo(Database database, DatabaseMetaData databaseMetaData)
      throws SQLException {
    this.tableNames = extractDatabaseTables(database, databaseMetaData);
    this.indexNames = extractDatabaseIndices(database, databaseMetaData);
    this.databaseMetaData = databaseMetaData;
  }

  /**
   * Returns the table names in the Spanner database.
   */
  public Set<Table> getAllTables() {
    return tableNames;
  }

  /**
   * Returns the names of all the indices in the Spanner database.
   */
  public Map<Table, Set<String>> getAllIndices() {
    return indexNames;
  }

  /**
   * Returns the names of all the imported foreign keys for a specified {@code tableName}.
   */
  public Set<String> getImportedForeignKeys(Table table) {
    try {
      HashSet<String> foreignKeys = new HashSet<>();

      ResultSet rs = databaseMetaData.getImportedKeys(
          table.getCatalog(), table.getSchema(), table.getName());
      while (rs.next()) {
        foreignKeys.add(rs.getString("FK_NAME"));
      }
      rs.close();
      return foreignKeys;
    } catch (SQLException e) {
      throw new RuntimeException(
          "Failed to lookup Spanner Database foreign keys for table: " + table, e);
    }
  }

  private static Set<Table> extractDatabaseTables(
      Database database, DatabaseMetaData databaseMetaData) throws SQLException {
    HashSet<Table> result = new HashSet<>();

    // Passing all null parameters will get all the tables and apply no filters.
    try (ResultSet resultSet = databaseMetaData.getTables(
        null, null, null, null)) {
      while (resultSet.next()) {
        String type = resultSet.getString("TABLE_TYPE");
        if (type.equals("TABLE")) {
          Table table = new Table("orm",
              database.locateNamespace(
                  Identifier.toIdentifier(resultSet.getString("TABLE_CAT")),
                  Identifier.toIdentifier(resultSet.getString("TABLE_SCHEM"))),
              Identifier.toIdentifier(resultSet.getString("TABLE_NAME")),
              false);
          result.add(table);
        }
      }
    }

    return result;
  }

  private static Map<Table, Set<String>> extractDatabaseIndices(
      Database database, DatabaseMetaData databaseMetaData) throws SQLException {
    HashMap<Table, Set<String>> result = new HashMap<>();
    try (ResultSet indexResultSet = databaseMetaData.getIndexInfo(
        null, null, null, false, false)) {

      while (indexResultSet.next()) {
        String name = indexResultSet.getString("INDEX_NAME");
        Table table = new Table("orm",
            database.locateNamespace(
                Identifier.toIdentifier(indexResultSet.getString("TABLE_CAT")),
                Identifier.toIdentifier(indexResultSet.getString("TABLE_SCHEM"))),
            Identifier.toIdentifier(indexResultSet.getString("TABLE_NAME")),
            false);
        Set<String> tableIndices = result.computeIfAbsent(table, k -> new HashSet<>());
        tableIndices.add(name);
      }
    }
    return result;
  }
}
