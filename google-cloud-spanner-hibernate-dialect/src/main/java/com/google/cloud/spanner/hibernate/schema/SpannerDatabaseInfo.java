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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Helper class for extracting information from the {@link DatabaseMetaData} which contains
 * information about what tables and indices currently exist in the database.
 */
public class SpannerDatabaseInfo {

  private final Set<String> tableNames;

  private final Set<String> indexNames;

  private final DatabaseMetaData databaseMetaData;

  /**
   * Constructs the {@link SpannerDatabaseInfo} by querying the Spanner database metadata.
   */
  public SpannerDatabaseInfo(DatabaseMetaData databaseMetaData) throws SQLException {
    this.tableNames = extractDatabaseTables(databaseMetaData);
    this.indexNames = extractDatabaseIndices(databaseMetaData);
    this.databaseMetaData = databaseMetaData;
  }

  /**
   * Returns the table names in the Spanner database.
   */
  public Set<String> getAllTables() {
    return tableNames;
  }

  /**
   * Returns the names of all the indices in the Spanner database.
   */
  public Set<String> getAllIndices() {
    return indexNames;
  }

  /**
   * Returns the names of all the imported foreign keys for a specified {@code tableName}.
   */
  public Set<String> getImportedForeignKeys(String tableName) {
    try {
      HashSet<String> foreignKeys = new HashSet<>();

      ResultSet rs = databaseMetaData.getImportedKeys(null, null, tableName);
      while (rs.next()) {
        foreignKeys.add(rs.getString("FK_NAME"));
      }
      rs.close();
      return foreignKeys;
    } catch (SQLException e) {
      throw new RuntimeException(
          "Failed to lookup Spanner Database foreign keys for table: " + tableName, e);
    }
  }

  private static Set<String> extractDatabaseTables(DatabaseMetaData databaseMetaData)
      throws SQLException {
    HashSet<String> result = new HashSet<String>();

    // Passing all null parameters will get all the tables and apply no filters.
    ResultSet resultSet = databaseMetaData.getTables(
        null, null, null, null);
    while (resultSet.next()) {
      String type = resultSet.getString("TABLE_TYPE");
      if (type.equals("TABLE")) {
        result.add(resultSet.getString("TABLE_NAME"));
      }
    }
    resultSet.close();

    return result;
  }

  private static Set<String> extractDatabaseIndices(DatabaseMetaData databaseMetaData)
      throws SQLException {
    HashSet<String> result = new HashSet<>();
    ResultSet indexResultSet = databaseMetaData.getIndexInfo(
        null, null, null, false, false);

    while (indexResultSet.next()) {
      String name = indexResultSet.getString("INDEX_NAME");
      result.add(name);
    }
    indexResultSet.close();

    return result;
  }
}
