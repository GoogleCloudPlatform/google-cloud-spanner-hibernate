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

package com.google.cloud.spanner.hibernate;

import com.google.common.collect.ImmutableList;
import com.mockrunner.mock.jdbc.MockDatabaseMetaData;
import com.mockrunner.mock.jdbc.MockResultSet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.mapping.Table;

/**
 * Helper class to building mock objects for the mock JDBC driver.
 */
public class MockJdbcUtils {

  private static final String[] TABLE_METADATA_COLUMNS = new String[]{
      "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "TABLE_TYPE", "REMARKS", "TYPE_CAT",
      "TYPE_SCHEM", "TYPE_NAME", "SELF_REFERENCING_COL_NAME", "REF"
  };

  private static final String[] COLUMN_METADATA_LABELS = new String[]{
      "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "DATA_TYPE",
      "TYPE_NAME", "COLUMN_SIZE", "DECIMAL_DIGITS", "IS_NULLABLE",
  };

  private static final String[] IMPORTED_KEY_COLUMNS = new String[]{
      "PKTABLE_CAT", "PKTABLE_SCHEM", "PKTABLE_NAME", "PKCOLUMN_NAME",
      "FKTABLE_CAT", "FKTABLE_SCHEM", "FKTABLE_NAME", "FKCOLUMN_NAME",
      "KEY_SEQ", "UPDATE_RULE", "DELETE_RULE", "FK_NAME", "PK_NAME",
      "DEFERRABILITY"
  };

  /**
   * Creates the metadata object read by Hibernate to determine which tables already exist.
   */
  public static MockDatabaseMetaDataBuilder metaDataBuilder() {
    return new MockDatabaseMetaDataBuilder();
  }

  /**
   * Constructs a mock Column metadata {@link ResultSet} describing fake columns of the tables.
   */
  private static ResultSet createColumnMetadataResultSet(Map<String, List<String>> columns) {
    MockResultSet mockResultSet = initResultSet(COLUMN_METADATA_LABELS);

    for (Entry<String, List<String>> entry : columns.entrySet()) {
      for (String column : entry.getValue()) {
        Object[] row = new Object[COLUMN_METADATA_LABELS.length];
        Arrays.fill(row, "");
        row[2] = entry.getKey();
        row[3] = column;
        row[4] = Types.VARCHAR;
        row[5] = "string(255)";
        row[6] = 255;
        row[7] = 0;
        row[8] = 0;
        mockResultSet.addRow(row);
      }
    }

    return mockResultSet;
  }


  /**
   * Constructs a mock Table metadata {@link ResultSet} describing table names.
   */
  private static ResultSet createTableMetadataResultSet(String... tableNames) {
    MockResultSet mockResultSet = initResultSet(TABLE_METADATA_COLUMNS);

    for (int i = 0; i < tableNames.length; i++) {
      String[] row = new String[TABLE_METADATA_COLUMNS.length];
      Arrays.fill(row, "");
      row[2] = tableNames[i];
      row[3] = "TABLE";
      mockResultSet.addRow(row);
    }

    return mockResultSet;
  }

  /**
   * Constructs a {@link MockResultSet} containing database metadata about indices for testing.
   */
  private static ResultSet createIndexMetadataResultSet(Table table, String... indexNames) {
    MockResultSet mockResultSet = initResultSet(
        "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "INDEX_NAME");

    for (int i = 0; i < indexNames.length; i++) {
      String[] row =
          new String[]{table.getCatalog(), table.getSchema(), table.getName(), indexNames[i]};
      mockResultSet.addRow(row);
    }

    return mockResultSet;

  }

  private static ResultSet createImportedKeysResultSet(
      String pkTable, String pkColumn, String fkTable, String fkColumn, String fkName) {

    String[] row = new String[IMPORTED_KEY_COLUMNS.length];
    Arrays.fill(row, "");
    row[0] = ""; // pk catalog
    row[1] = ""; // fk catalog
    row[2] = pkTable;
    row[3] = pkColumn;
    row[4] = ""; // fk catalog
    row[5] = ""; // fk schema
    row[6] = fkTable;
    row[7] = fkColumn;
    row[11] = fkName;
    MockResultSet mockResultSet = initResultSet(IMPORTED_KEY_COLUMNS);
    mockResultSet.addRow(row);

    return mockResultSet;
  }

  private static ResettingMockResultSet initResultSet(String... columnLabels) {
    ResettingMockResultSet mockResultSet =
        new ResettingMockResultSet(UUID.randomUUID().toString());
    for (int i = 0; i < columnLabels.length; i++) {
      mockResultSet.addColumn(columnLabels[i]);
    }

    return mockResultSet;
  }

  /**
   * A builder to help build the mock JDBC metadata objects.
   */
  public static class MockDatabaseMetaDataBuilder {

    private ResultSet columns = new MockResultSet(UUID.randomUUID().toString());
    private ResultSet tables = new MockResultSet(UUID.randomUUID().toString());
    private ResultSet indexInfo = new MockResultSet(UUID.randomUUID().toString());
    private ResultSet importedKeys = new MockResultSet(UUID.randomUUID().toString());
    private ResultSet exportedKeys = new MockResultSet(UUID.randomUUID().toString());

    /** Sets which tables are present in the Spanner database. */
    public MockDatabaseMetaDataBuilder setTables(String... tables) {
      this.tables = createTableMetadataResultSet(tables);
      this.columns =
          createColumnMetadataResultSet(
              Arrays.stream(tables)
                  .map(t -> new String[] {t, "column"})
                  .collect(
                      Collectors.<String[], String, List<String>>toMap(
                          c -> c[0], c -> ImmutableList.of(c[1]))));
      return this;
    }

    /** Sets which tables with which columns are present in the Spanner database. */
    public MockDatabaseMetaDataBuilder setTables(Map<String, List<String>> tablesAndColumns) {
      this.tables = createTableMetadataResultSet(tablesAndColumns.keySet().toArray(new String[0]));
      this.columns = createColumnMetadataResultSet(tablesAndColumns);
      return this;
    }

    /**
     * Sets which indices are present in the Spanner database.
     */
    public MockDatabaseMetaDataBuilder setIndices(Table table, String... indices) {
      this.indexInfo = createIndexMetadataResultSet(table, indices);
      return this;
    }

    /** Sets the imported keys that should be returned. */
    public MockDatabaseMetaDataBuilder setImportedKeys(
        String pkTable, String pkColumn, String fkTable, String fkColumn, String fkName) {
      this.importedKeys = createImportedKeysResultSet(pkTable, pkColumn, fkTable, fkColumn, fkName);
      return this;
    }

    /**
     * Builds the {@link MockDatabaseMetaData} object which is used by Hibernate to get information
     * about the Spanner Database.
     */
    public MockDatabaseMetaData build() {
      MockDatabaseMetaData mockDatabaseMetaData = new MockDatabaseMetaData();
      mockDatabaseMetaData.setStoresLowerCaseIdentifiers(false);
      mockDatabaseMetaData.setStoresUpperCaseIdentifiers(false);
      mockDatabaseMetaData.setStoresMixedCaseIdentifiers(true);
      mockDatabaseMetaData.setStoresLowerCaseQuotedIdentifiers(false);
      mockDatabaseMetaData.setStoresUpperCaseQuotedIdentifiers(false);
      mockDatabaseMetaData.setStoresMixedCaseIdentifiers(true);
      mockDatabaseMetaData.setColumns(columns);
      mockDatabaseMetaData.setTables(tables);
      mockDatabaseMetaData.setIndexInfo(indexInfo);
      mockDatabaseMetaData.setImportedKeys(importedKeys);
      mockDatabaseMetaData.setExportedKeys(exportedKeys);
      return mockDatabaseMetaData;
    }
  }


  /**
   * An extension of {@link MockResultSet} which resets the cursor when it is closed. It is useful
   * for testing purposes and emulating the creating of a new {@link ResultSet} when new metadata
   * queries are made.
   */
  private static class ResettingMockResultSet extends MockResultSet {

    public ResettingMockResultSet(String id) {
      super(id);
    }

    @Override
    public void close() throws SQLException {
      super.close();
      beforeFirst();
    }
  }
}
