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
import org.hibernate.boot.model.relational.Namespace;
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
   *
   * @param database The Hibernate database model.
   * @param databaseMetaData The JDBC metadata.
   * @param defaultSchema The configured default schema (from hibernate.default_schema) can be null.
   */
  public SpannerDatabaseInfo(
      Database database, DatabaseMetaData databaseMetaData, String defaultSchema)
      throws SQLException {
    this.tableNames = extractDatabaseTables(database, databaseMetaData, defaultSchema);
    this.indexNames = extractDatabaseIndices(database, databaseMetaData, defaultSchema);
    this.databaseMetaData = databaseMetaData;
  }

  /** Returns the table names in the Spanner database. */
  public Set<Table> getAllTables() {
    return tableNames;
  }

  /** Returns the names of all the indices in the Spanner database. */
  public Map<Table, Set<String>> getAllIndices() {
    return indexNames;
  }

  /** Returns the names of all the imported foreign keys for a specified {@code tableName}. */
  public Set<String> getImportedForeignKeys(Table table) {
    try {
      HashSet<String> foreignKeys = new HashSet<>();

      ResultSet rs =
          databaseMetaData.getImportedKeys(table.getCatalog(), table.getSchema(), table.getName());
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
      Database database, DatabaseMetaData databaseMetaData, String defaultSchema)
      throws SQLException {
    HashSet<Table> result = new HashSet<>();

    // Iterate over all namespaces that belong to the application.
    for (Namespace namespace : database.getNamespaces()) {

      String schema = resolveSchemaName(namespace, defaultSchema);

      try (ResultSet resultSet = databaseMetaData.getTables(null, schema, null, null)) {
        while (resultSet.next()) {
          String type = resultSet.getString("TABLE_TYPE");
          if ("TABLE".equals(type)) {
            Table table =
                new Table(
                    "orm",
                    namespace,
                    Identifier.toIdentifier(resultSet.getString("TABLE_NAME")),
                    false);
            result.add(table);
          }
        }
      }
    }

    return result;
  }

  private static Map<Table, Set<String>> extractDatabaseIndices(
      Database database, DatabaseMetaData databaseMetaData, String defaultSchema)
      throws SQLException {
    HashMap<Table, Set<String>> result = new HashMap<>();

    for (Namespace namespace : database.getNamespaces()) {

      String schema = resolveSchemaName(namespace, defaultSchema);

      try (ResultSet indexResultSet =
          databaseMetaData.getIndexInfo(null, schema, null, false, false)) {

        while (indexResultSet.next()) {
          String name = indexResultSet.getString("INDEX_NAME");
          Table table =
              new Table(
                  "orm",
                  namespace,
                  Identifier.toIdentifier(indexResultSet.getString("TABLE_NAME")),
                  false);
          Set<String> tableIndices = result.computeIfAbsent(table, k -> new HashSet<>());
          tableIndices.add(name);
        }
      }
    }
    return result;
  }

  private static String resolveSchemaName(Namespace namespace, String defaultSchema) {
    if (namespace.getPhysicalName().schema() != null) {
      return namespace.getPhysicalName().schema().render();
    }
    if (defaultSchema != null) {
      return defaultSchema;
    }
    return "";
  }
}
