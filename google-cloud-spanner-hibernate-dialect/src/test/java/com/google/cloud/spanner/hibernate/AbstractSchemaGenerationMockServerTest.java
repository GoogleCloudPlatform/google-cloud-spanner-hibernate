/*
 * Copyright 2019-2023 Google LLC
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

import com.google.cloud.spanner.Statement;
import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Value;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.Type;
import com.google.spanner.v1.TypeCode;

public class AbstractSchemaGenerationMockServerTest extends AbstractMockSpannerServerTest {
  protected static final Statement GET_TABLES_STATEMENT = Statement.newBuilder(
          "SELECT TABLE_CATALOG AS TABLE_CAT, TABLE_SCHEMA AS TABLE_SCHEM, TABLE_NAME,\n"
              + "       CASE WHEN TABLE_TYPE = 'BASE TABLE' THEN 'TABLE' ELSE TABLE_TYPE END AS TABLE_TYPE,\n"
              + "       NULL AS REMARKS, NULL AS TYPE_CAT, NULL AS TYPE_SCHEM, NULL AS TYPE_NAME,\n"
              + "       NULL AS SELF_REFERENCING_COL_NAME, NULL AS REF_GENERATION\n"
              + "FROM INFORMATION_SCHEMA.TABLES AS T\n"
              + "WHERE UPPER(TABLE_CATALOG) LIKE @p1\n"
              + "  AND UPPER(TABLE_SCHEMA) LIKE @p2\n"
              + "  AND UPPER(TABLE_NAME) LIKE @p3\n"
              + "  AND (\n"
              + "            (CASE WHEN TABLE_TYPE = 'BASE TABLE' THEN 'TABLE' ELSE TABLE_TYPE END) LIKE @p4\n"
              + "        OR\n"
              + "            (CASE WHEN TABLE_TYPE = 'BASE TABLE' THEN 'TABLE' ELSE TABLE_TYPE END) LIKE @p5\n"
              + "    )\n"
              + "ORDER BY TABLE_CATALOG, TABLE_SCHEMA, TABLE_NAME")
      .bind("p1")
      .to("%")
      .bind("p2")
      .to("%")
      .bind("p3")
      .to("%")
      .bind("p4")
      .to("TABLE")
      .bind("p5")
      .to("VIEW")
      .build();
  protected static final ResultSetMetadata GET_TABLES_METADATA = ResultSetMetadata.newBuilder()
      .setRowType(StructType.newBuilder()
          .addFields(Field.newBuilder().setName("TABLE_CAT").setType(
              Type.newBuilder().setCode(TypeCode.STRING).build()).build())
          .addFields(Field.newBuilder().setName("TABLE_SCHEM").setType(Type.newBuilder().setCode(TypeCode.STRING).build()).build())
          .addFields(Field.newBuilder().setName("TABLE_NAME").setType(Type.newBuilder().setCode(TypeCode.STRING).build()).build())
          .addFields(Field.newBuilder().setName("TABLE_TYPE").setType(Type.newBuilder().setCode(TypeCode.STRING).build()).build())
          .addFields(Field.newBuilder().setName("REMARKS").setType(Type.newBuilder().setCode(TypeCode.STRING).build()).build())
          .addFields(Field.newBuilder().setName("TYPE_CAT").setType(Type.newBuilder().setCode(TypeCode.STRING).build()).build())
          .addFields(Field.newBuilder().setName("TYPE_SCHEM").setType(Type.newBuilder().setCode(TypeCode.STRING).build()).build())
          .addFields(Field.newBuilder().setName("TYPE_NAME").setType(Type.newBuilder().setCode(TypeCode.STRING).build()).build())
          .addFields(Field.newBuilder().setName("SELF_REFERENCING_COLUMN_NAME").setType(Type.newBuilder().setCode(TypeCode.STRING).build()).build())
          .addFields(Field.newBuilder().setName("REF_GENERATION").setType(Type.newBuilder().setCode(TypeCode.STRING).build()).build())
          .build())
      .build();
  protected static ListValue createTableRow(String tableName) {
    return createTableOrViewRow(tableName, "TABLE");
  }

  protected static ListValue createViewRow(String viewName) {
    return createTableOrViewRow(viewName, "VIEW");
  }

  protected static ListValue createTableOrViewRow(String tableName, String tableType) {
    return ListValue.newBuilder()
        .addValues(Value.newBuilder().setStringValue("").build())
        .addValues(Value.newBuilder().setStringValue("").build())
        .addValues(Value.newBuilder().setStringValue(tableName).build())
        .addValues(Value.newBuilder().setStringValue(tableType).build())
        .addValues(Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build())
        .addValues(Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build())
        .addValues(Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build())
        .addValues(Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build())
        .addValues(Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build())
        .addValues(Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build())
        .build();
  }

  protected static final Statement GET_COLUMNS_STATEMENT = Statement.of("SELECT TABLE_CATALOG AS TABLE_CAT, TABLE_SCHEMA AS TABLE_SCHEM, TABLE_NAME, COLUMN_NAME,\n"
      + "  CASE\n"
      + "    WHEN SPANNER_TYPE LIKE 'ARRAY%' THEN 2003\n"
      + "    WHEN SPANNER_TYPE = 'BOOL' THEN 16\n"
      + "    WHEN SPANNER_TYPE LIKE 'BYTES%' THEN -2\n"
      + "    WHEN SPANNER_TYPE = 'DATE' THEN 91\n"
      + "    WHEN SPANNER_TYPE = 'FLOAT64' THEN 8\n"
      + "    WHEN SPANNER_TYPE = 'INT64' THEN -5\n"
      + "    WHEN SPANNER_TYPE = 'NUMERIC' THEN 2\n"
      + "    WHEN SPANNER_TYPE LIKE 'STRING%' THEN -9\n"
      + "    WHEN SPANNER_TYPE = 'JSON' THEN -9\n"
      + "    WHEN SPANNER_TYPE = 'TIMESTAMP' THEN 93\n"
      + "  END AS DATA_TYPE,\n"
      + "  SPANNER_TYPE AS TYPE_NAME,\n"
      + "  CASE\n"
      + "    WHEN STRPOS(SPANNER_TYPE, '(')=0 THEN\n"
      + "      CASE\n"
      + "        WHEN SPANNER_TYPE = 'INT64' OR SPANNER_TYPE = 'ARRAY<INT64>' THEN 19\n"
      + "        WHEN SPANNER_TYPE = 'NUMERIC' OR SPANNER_TYPE = 'ARRAY<NUMERIC>' THEN 15\n"
      + "        WHEN SPANNER_TYPE = 'FLOAT64' OR SPANNER_TYPE = 'ARRAY<FLOAT64>' THEN 15\n"
      + "        WHEN SPANNER_TYPE = 'BOOL' OR SPANNER_TYPE = 'ARRAY<BOOL>' THEN NULL\n"
      + "        WHEN SPANNER_TYPE = 'DATE' OR SPANNER_TYPE = 'ARRAY<DATE>' THEN 10\n"
      + "        WHEN SPANNER_TYPE = 'TIMESTAMP' OR SPANNER_TYPE = 'ARRAY<TIMESTAMP>' THEN 35\n"
      + "        WHEN SPANNER_TYPE = 'JSON' OR SPANNER_TYPE = 'ARRAY<JSON>' THEN 2621440\n"
      + "        ELSE 0\n"
      + "      END\n"
      + "    ELSE CAST(REPLACE(SUBSTR(SPANNER_TYPE, STRPOS(SPANNER_TYPE, '(')+1, STRPOS(SPANNER_TYPE, ')')-STRPOS(SPANNER_TYPE, '(')-1), 'MAX', CASE WHEN UPPER(SPANNER_TYPE) LIKE '%STRING%' THEN '2621440' ELSE '10485760' END) AS INT64)\n"
      + "  END AS COLUMN_SIZE,\n"
      + "  0 AS BUFFER_LENGTH,\n"
      + "  CASE\n"
      + "    WHEN SPANNER_TYPE LIKE '%FLOAT64%' THEN 16\n"
      + "    ELSE NULL\n"
      + "  END AS DECIMAL_DIGITS,\n"
      + "  CASE\n"
      + "    WHEN SPANNER_TYPE LIKE '%INT64%' THEN 10\n"
      + "    WHEN SPANNER_TYPE LIKE '%NUMERIC%' THEN 10\n"
      + "    WHEN SPANNER_TYPE LIKE '%FLOAT64%' THEN 2\n"
      + "    ELSE NULL\n"
      + "  END AS NUM_PREC_RADIX,\n"
      + "  CASE\n"
      + "    WHEN IS_NULLABLE = 'YES' THEN 1\n"
      + "    WHEN IS_NULLABLE = 'NO' THEN 0\n"
      + "    ELSE 2\n"
      + "  END AS NULLABLE,\n"
      + "  NULL AS REMARKS,\n"
      + "  NULL AS COLUMN_DEF,\n"
      + "  0 AS SQL_DATA_TYPE,\n"
      + "  0 AS SQL_DATETIME_SUB,\n"
      + "  CASE\n"
      + "    WHEN (SPANNER_TYPE LIKE 'STRING%' OR SPANNER_TYPE LIKE 'ARRAY<STRING%') THEN\n"
      + "      cast(replace(substr(spanner_type, strpos(spanner_type, '(')+1, strpos(spanner_type,')')-strpos(spanner_type, '(')-1), 'MAX', '2621440') as INT64)\n"
      + "    WHEN (SPANNER_TYPE = 'JSON' OR SPANNER_TYPE = 'ARRAY<JSON>') THEN 2621440\n"
      + "    ELSE NULL\n"
      + "  END AS CHAR_OCTET_LENGTH,\n"
      + "  ORDINAL_POSITION,\n"
      + "  IS_NULLABLE,\n"
      + "  NULL AS SCOPE_CATALOG,\n"
      + "  NULL AS SCOPE_SCHEMA,\n"
      + "  NULL AS SCOPE_TABLE,\n"
      + "  NULL AS SOURCE_DATA_TYPE,\n"
      + "  'NO' AS IS_AUTOINCREMENT,\n"
      + "  CASE\n"
      + "    WHEN (IS_GENERATED = 'NEVER') THEN 'NO'\n"
      + "    ELSE 'YES'\n"
      + "  END AS IS_GENERATEDCOLUMN\n"
      + "FROM INFORMATION_SCHEMA.COLUMNS C\n"
      + "WHERE UPPER(C.TABLE_CATALOG) LIKE @p1\n"
      + "  AND UPPER(C.TABLE_SCHEMA) LIKE @p2\n"
      + "  AND UPPER(C.TABLE_NAME) LIKE @p3\n"
      + "  AND UPPER(C.COLUMN_NAME) LIKE @p4\n"
      + "ORDER BY TABLE_CATALOG, TABLE_SCHEMA, TABLE_NAME, ORDINAL_POSITION");

  protected static final Statement GET_INDEXES_STATEMENT = Statement.newBuilder(
          "SELECT IDX.TABLE_CATALOG AS TABLE_CAT, IDX.TABLE_SCHEMA AS TABLE_SCHEM, IDX.TABLE_NAME,\n"
              + "  CASE WHEN IS_UNIQUE THEN FALSE ELSE TRUE END AS NON_UNIQUE,\n"
              + "  IDX.TABLE_CATALOG AS INDEX_QUALIFIER, IDX.INDEX_NAME,\n"
              + "  CASE WHEN IDX.INDEX_NAME = 'PRIMARY_KEY' THEN 1 ELSE 2 END AS TYPE,\n"
              + "  ORDINAL_POSITION, COLUMN_NAME, SUBSTR(COLUMN_ORDERING, 1, 1) AS ASC_OR_DESC,\n"
              + "  -1 AS CARDINALITY, \n"
              + "  -1 AS PAGES, \n"
              + "  NULL AS FILTER_CONDITION\n"
              + "FROM INFORMATION_SCHEMA.INDEXES IDX\n"
              + "INNER JOIN INFORMATION_SCHEMA.INDEX_COLUMNS COL\n"
              + "  ON  IDX.TABLE_CATALOG=COL.TABLE_CATALOG\n"
              + "  AND IDX.TABLE_SCHEMA=COL.TABLE_SCHEMA\n"
              + "  AND IDX.TABLE_NAME=COL.TABLE_NAME\n"
              + "  AND IDX.INDEX_NAME=COL.INDEX_NAME\n"
              + "WHERE UPPER(IDX.TABLE_CATALOG) LIKE @p1\n"
              + "  AND UPPER(IDX.TABLE_SCHEMA) LIKE @p2\n"
              + "  AND UPPER(IDX.TABLE_NAME) LIKE @p3\n"
              + "  AND UPPER(IDX.INDEX_NAME) LIKE @p4\n"
              + "  AND (CASE WHEN IS_UNIQUE THEN 'YES' ELSE 'NO' END) LIKE @p5\n"
              + "ORDER BY IDX.TABLE_NAME, IS_UNIQUE DESC, IDX.INDEX_NAME, CASE WHEN ORDINAL_POSITION IS NULL THEN 0 ELSE ORDINAL_POSITION END")
      .bind("p1")
      .to("%")
      .bind("p2")
      .to("%")
      .bind("p3")
      .to("%")
      .bind("p4")
      .to("%")
      .bind("p5")
      .to("%")
      .build();

  protected static final Statement GET_SEQUENCES_STATEMENT = Statement.of(
      "select catalog as sequence_catalog, "
          + "schema as sequence_schema, "
          + "name as sequence_name, "
          + "1 as start_value, "
          + "1 as minimum_value, "
          + "9223372036854775807 as maximum_value, "
          + "1 as increment "
          + "from information_schema.sequences");
  protected static final ResultSetMetadata GET_SEQUENCES_METADATA = ResultSetMetadata.newBuilder()
      .setRowType(StructType.newBuilder()
          .addFields(Field.newBuilder().setName("sequence_catalog").setType(Type.newBuilder().setCode(TypeCode.STRING).build()).build())
          .addFields(Field.newBuilder().setName("sequence_schema").setType(Type.newBuilder().setCode(TypeCode.STRING).build()).build())
          .addFields(Field.newBuilder().setName("sequence_name").setType(Type.newBuilder().setCode(TypeCode.STRING).build()).build())
          .addFields(Field.newBuilder().setName("start_value").setType(Type.newBuilder().setCode(TypeCode.INT64).build()).build())
          .addFields(Field.newBuilder().setName("minimum_value").setType(Type.newBuilder().setCode(TypeCode.INT64).build()).build())
          .addFields(Field.newBuilder().setName("maximum_value").setType(Type.newBuilder().setCode(TypeCode.INT64).build()).build())
          .addFields(Field.newBuilder().setName("increment").setType(Type.newBuilder().setCode(TypeCode.INT64).build()).build())
          .build())
      .build();
  protected static ListValue createSequenceRow(String sequenceName) {
    return ListValue.newBuilder()
        .addValues(Value.newBuilder().setStringValue("").build())
        .addValues(Value.newBuilder().setStringValue("").build())
        .addValues(Value.newBuilder().setStringValue(sequenceName).build())
        .addValues(Value.newBuilder().setStringValue("1").build())
        .addValues(Value.newBuilder().setStringValue("1").build())
        .addValues(Value.newBuilder().setStringValue("9223372036854775807").build())
        .addValues(Value.newBuilder().setStringValue("1").build())
        .build();
  }

}
