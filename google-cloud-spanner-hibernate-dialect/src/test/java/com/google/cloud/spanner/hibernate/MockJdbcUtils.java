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

import com.mockrunner.mock.jdbc.MockDatabaseMetaData;
import com.mockrunner.mock.jdbc.MockResultSet;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Arrays;
import java.util.UUID;

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

  /**
   * Creates the metadata object read by Hibernate to determine which tables already exist.
   */
  public static MockDatabaseMetaData createMockDatabaseMetaData(String... tables) {
    MockDatabaseMetaData mockDatabaseMetaData = new MockDatabaseMetaData();
    mockDatabaseMetaData.setColumns(createColumnMetadataResultSet(tables));
    mockDatabaseMetaData.setTables(createTableMetadataResultSet(tables));
    mockDatabaseMetaData.setIndexInfo(new MockResultSet(UUID.randomUUID().toString()));
    mockDatabaseMetaData.setImportedKeys(new MockResultSet(UUID.randomUUID().toString()));
    return mockDatabaseMetaData;
  }

  /**
   * Constructs a mock Column metadata {@link ResultSet} describing fake columns of the tables.
   */
  private static ResultSet createColumnMetadataResultSet(String... tableNames) {
    MockResultSet mockResultSet = initResultSet(COLUMN_METADATA_LABELS);

    for (int i = 0; i < tableNames.length; i++) {
      Object[] row = new Object[COLUMN_METADATA_LABELS.length];
      Arrays.fill(row, "");
      row[2] = tableNames[i];
      row[3] = "column";
      row[4] = Types.VARCHAR;
      row[5] = "string(255)";
      row[6] = 255;
      row[7] = 0;
      row[8] = 0;
      mockResultSet.addRow(row);
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

  private static MockResultSet initResultSet(String[] columnLabels) {
    MockResultSet mockResultSet = new MockResultSet(UUID.randomUUID().toString());
    for (int i = 0; i < columnLabels.length; i++) {
      mockResultSet.addColumn(columnLabels[i]);
    }

    return mockResultSet;
  }
}
