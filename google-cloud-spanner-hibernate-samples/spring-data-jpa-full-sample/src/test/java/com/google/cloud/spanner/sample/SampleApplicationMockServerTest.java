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

package com.google.cloud.spanner.sample;

import static junit.framework.TestCase.assertEquals;

import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.SpannerOptionsHelper;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.connection.AbstractMockServerTest;
import com.google.common.base.Strings;
import com.google.longrunning.Operation;
import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Empty;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ResultSet;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.Type;
import com.google.spanner.v1.TypeCode;
import io.opentelemetry.api.GlobalOpenTelemetry;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Runs the {@link SampleApplication} and verifies that it sends the expected requests. */
@RunWith(JUnit4.class)
public class SampleApplicationMockServerTest extends AbstractMockServerTest {

  private static ResultSet empty() {
    return ResultSet.newBuilder()
        .setMetadata(
            ResultSetMetadata.newBuilder().setRowType(StructType.newBuilder().build()).build())
        .build();
  }

  /** Adds the expected query results to the mock server. */
  @BeforeClass
  public static void setupQueryResults() {
    // Add a DDL response to the server.
    addDdlResponseToSpannerAdmin();

    // Set the database dialect.
    mockSpanner.putStatementResult(
        StatementResult.detectDialectResult(Dialect.GOOGLE_STANDARD_SQL));

    // Setup empty results for the metadata queries that check whether the schema has been created.
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of(
                "select seq.CATALOG as sequence_catalog, seq.SCHEMA as sequence_schema, seq.NAME as sequence_name,\n"
                    + "       coalesce(kind.OPTION_VALUE, 'bit_reversed_positive') as KIND,\n"
                    + "       coalesce(safe_cast(initial.OPTION_VALUE AS INT64),\n"
                    + "           case coalesce(kind.OPTION_VALUE, 'bit_reversed_positive')\n"
                    + "               when 'bit_reversed_positive' then 1\n"
                    + "               when 'bit_reversed_signed' then -pow(2, 63)\n"
                    + "               else 1\n"
                    + "           end\n"
                    + "       ) as start_value, 1 as minimum_value, 9223372036854775807 as maximum_value,\n"
                    + "       1 as increment,\n"
                    + "       safe_cast(skip_range_min.OPTION_VALUE as int64) as skip_range_min,\n"
                    + "       safe_cast(skip_range_max.OPTION_VALUE as int64) as skip_range_max,\n"
                    + "from INFORMATION_SCHEMA.SEQUENCES seq\n"
                    + "left outer join INFORMATION_SCHEMA.SEQUENCE_OPTIONS kind\n"
                    + "    on seq.CATALOG=kind.CATALOG and seq.SCHEMA=kind.SCHEMA and seq.NAME=kind.NAME and kind.OPTION_NAME='sequence_kind'\n"
                    + "left outer join INFORMATION_SCHEMA.SEQUENCE_OPTIONS initial\n"
                    + "    on seq.CATALOG=initial.CATALOG and seq.SCHEMA=initial.SCHEMA and seq.NAME=initial.NAME and initial.OPTION_NAME='start_with_counter'\n"
                    + "left outer join INFORMATION_SCHEMA.SEQUENCE_OPTIONS skip_range_min\n"
                    + "    on seq.CATALOG=skip_range_min.CATALOG and seq.SCHEMA=skip_range_min.SCHEMA and seq.NAME=skip_range_min.NAME and skip_range_min.OPTION_NAME='skip_range_min'\n"
                    + "left outer join INFORMATION_SCHEMA.SEQUENCE_OPTIONS skip_range_max\n"
                    + "    on seq.CATALOG=skip_range_max.CATALOG and seq.SCHEMA=skip_range_max.SCHEMA and seq.NAME=skip_range_max.NAME and skip_range_max.OPTION_NAME='skip_range_max'"),
            empty()));
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.newBuilder(
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
                .build(),
            empty()));
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.newBuilder(
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
                .build(),
            empty()));
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.newBuilder(
                    "SELECT TABLE_CATALOG AS TABLE_CAT, TABLE_SCHEMA AS TABLE_SCHEM, TABLE_NAME, COLUMN_NAME,\n"
                        + "  CASE\n"
                        + "    WHEN SPANNER_TYPE LIKE 'ARRAY%' THEN 2003\n"
                        + "    WHEN SPANNER_TYPE = 'BOOL' THEN 16\n"
                        + "    WHEN SPANNER_TYPE LIKE 'BYTES%' THEN -2\n"
                        + "    WHEN SPANNER_TYPE = 'DATE' THEN 91\n"
                        + "    WHEN SPANNER_TYPE = 'FLOAT64' THEN 8\n"
                        + "    WHEN SPANNER_TYPE = 'FLOAT32' THEN 7\n"
                        + "    WHEN SPANNER_TYPE = 'INT64' THEN -5\n"
                        + "    WHEN SPANNER_TYPE = 'NUMERIC' THEN 2\n"
                        + "    WHEN SPANNER_TYPE LIKE 'STRING%' THEN -9\n"
                        + "    WHEN SPANNER_TYPE = 'JSON' THEN -9\n"
                        + "    WHEN SPANNER_TYPE = 'TIMESTAMP' THEN 93\n"
                        + "    WHEN SPANNER_TYPE = 'UUID' THEN 1111\n"
                        + "    ELSE 1111\n"
                        + "  END AS DATA_TYPE,\n"
                        + "  SPANNER_TYPE AS TYPE_NAME,\n"
                        + "  CASE\n"
                        + "    WHEN STRPOS(SPANNER_TYPE, '(')=0 THEN\n"
                        + "      CASE\n"
                        + "        WHEN SPANNER_TYPE = 'INT64' OR SPANNER_TYPE = 'ARRAY<INT64>' THEN 19\n"
                        + "        WHEN SPANNER_TYPE = 'NUMERIC' OR SPANNER_TYPE = 'ARRAY<NUMERIC>' THEN 15\n"
                        + "        WHEN SPANNER_TYPE = 'FLOAT64' OR SPANNER_TYPE = 'ARRAY<FLOAT64>' THEN 15\n"
                        + "        WHEN SPANNER_TYPE = 'FLOAT32' OR SPANNER_TYPE = 'ARRAY<FLOAT32>' THEN 15\n"
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
                        + "    WHEN SPANNER_TYPE LIKE '%FLOAT32%' THEN 16\n"
                        + "    ELSE NULL\n"
                        + "  END AS DECIMAL_DIGITS,\n"
                        + "  CASE\n"
                        + "    WHEN SPANNER_TYPE LIKE '%INT64%' THEN 10\n"
                        + "    WHEN SPANNER_TYPE LIKE '%NUMERIC%' THEN 10\n"
                        + "    WHEN SPANNER_TYPE LIKE '%FLOAT64%' THEN 2\n"
                        + "    WHEN SPANNER_TYPE LIKE '%FLOAT32%' THEN 2\n"
                        + "    ELSE NULL\n"
                        + "  END AS NUM_PREC_RADIX,\n"
                        + "  CASE\n"
                        + "    WHEN IS_NULLABLE = 'YES' THEN 1\n"
                        + "    WHEN IS_NULLABLE = 'NO' THEN 0\n"
                        + "    ELSE 2\n"
                        + "  END AS NULLABLE,\n"
                        + "  NULL AS REMARKS,\n"
                        + "  COLUMN_DEFAULT AS COLUMN_DEF,\n"
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
                        + "  IS_IDENTITY AS IS_AUTOINCREMENT,\n"
                        + "  CASE\n"
                        + "    WHEN (IS_GENERATED = 'NEVER') THEN 'NO'\n"
                        + "    ELSE 'YES'\n"
                        + "  END AS IS_GENERATEDCOLUMN\n"
                        + "FROM INFORMATION_SCHEMA.COLUMNS C\n"
                        + "WHERE UPPER(C.TABLE_CATALOG) LIKE @p1\n"
                        + "  AND UPPER(C.TABLE_SCHEMA) LIKE @p2\n"
                        + "  AND UPPER(C.TABLE_NAME) LIKE @p3\n"
                        + "  AND UPPER(C.COLUMN_NAME) LIKE @p4\n"
                        + "ORDER BY TABLE_CATALOG, TABLE_SCHEMA, TABLE_NAME, ORDINAL_POSITION")
                .bind("p1")
                .to("%")
                .bind("p2")
                .to("%")
                .bind("p3")
                .to("%")
                .bind("p4")
                .to("%")
                .build(),
            empty()));

    // Add results for the initial queries that are used to delete all data.
    mockSpanner.putStatementResults(
        StatementResult.query(
            Statement.of(
                "select ts1_0.id,ts1_0.concert_id,ts1_0.created_at,ts1_0.customer_name,ts1_0.price,ts1_0.seats,ts1_0.updated_at from ticket_sale ts1_0"),
            empty()));
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of(
                "select c1_0.id,c1_0.created_at,c1_0.end_time,c1_0.name,c1_0.singer_id,c1_0.start_time,c1_0.updated_at,c1_0.venue_id from concert c1_0"),
            empty()));
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of(
                "select a1_0.id,a1_0.cover_picture,a1_0.created_at,a1_0.marketing_budget,a1_0.release_date,a1_0.singer_id,a1_0.title,a1_0.updated_at from album a1_0"),
            empty()));
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of(
                "select s1_0.id,s1_0.active,s1_0.created_at,s1_0.first_name,s1_0.full_name,s1_0.last_name,s1_0.nick_names,s1_0.updated_at from singer s1_0"),
            empty()));

    // Add results for the insert statements.
    mockSpanner.putPartialStatementResult(
        StatementResult.update(
            Statement.of(
                "insert into singer (created_at,first_name,last_name,nick_names,updated_at,id) values (@p1,@p2,@p3,@p4,@p5,@p6)"),
            1L));
    mockSpanner.putPartialStatementResult(
        StatementResult.update(
            Statement.of(
                "insert into album (cover_picture,created_at,marketing_budget,release_date,singer_id,title,updated_at,id) values (@p1,@p2,@p3,@p4,@p5,@p6,@p7,@p8)"),
            1L));
    mockSpanner.putPartialStatementResult(
        StatementResult.update(
            Statement.of(
                "insert into track (created_at,sample_rate,title,updated_at,id,track_number) values (@p1,@p2,@p3,@p4,@p5,@p6)"),
            1L));
    mockSpanner.putPartialStatementResult(
        StatementResult.update(
            Statement.of(
                "insert into venue (created_at,description,name,updated_at,id) values (@p1,@p2,@p3,@p4,@p5)"),
            1L));
    mockSpanner.putPartialStatementResult(
        StatementResult.update(
            Statement.of("update venue set description=@p1,updated_at=@p2 where id=@p3"), 1L));
    mockSpanner.putPartialStatementResult(
        StatementResult.update(
            Statement.of(
                "update venue set created_at=@p1,description=@p2,updated_at=@p3 where id=@p4"),
            1L));
    mockSpanner.putPartialStatementResult(
        StatementResult.update(
            Statement.of(
                "insert into concert (created_at,end_time,name,singer_id,start_time,updated_at,venue_id,id) values (@p1,@p2,@p3,@p4,@p5,@p6,@p7,@p8)"),
            1L));
    mockSpanner.putPartialStatementResult(
        StatementResult.update(
            Statement.of(
                "insert into ticket_sale (concert_id,created_at,customer_name,price,seats,updated_at,id) values (@p1,@p2,@p3,@p4,@p5,@p6,@p7)"),
            1L));

    // Add results for generating ticket sale identifiers.
    mockSpanner.putStatementResult(
        StatementResult.queryAndThen(
            Statement.of(
                "/* spanner.force_read_write_transaction=true */ /* spanner.ignore_during_internal_retry=true */  select get_next_sequence_value(sequence ticket_sale_seq) AS n from unnest(generate_array(1, 200))"),
            generateSequenceResultSet(1L, 200L),
            generateSequenceResultSet(201L, 400L)));

    // Add results for selecting singers.
    UUID singerId = UUID.randomUUID();
    ResultSet singerResultSet =
        ResultSet.newBuilder()
            .setMetadata(
                ResultSetMetadata.newBuilder()
                    .setRowType(
                        StructType.newBuilder()
                            .addFields(
                                Field.newBuilder()
                                    .setName("id")
                                    .setType(Type.newBuilder().setCode(TypeCode.STRING).build())
                                    .build())
                            .addFields(
                                Field.newBuilder()
                                    .setName("active")
                                    .setType(Type.newBuilder().setCode(TypeCode.BOOL).build())
                                    .build())
                            .addFields(
                                Field.newBuilder()
                                    .setName("created_at")
                                    .setType(Type.newBuilder().setCode(TypeCode.TIMESTAMP).build())
                                    .build())
                            .addFields(
                                Field.newBuilder()
                                    .setName("first_name")
                                    .setType(Type.newBuilder().setCode(TypeCode.STRING).build())
                                    .build())
                            .addFields(
                                Field.newBuilder()
                                    .setName("full_name")
                                    .setType(Type.newBuilder().setCode(TypeCode.STRING).build())
                                    .build())
                            .addFields(
                                Field.newBuilder()
                                    .setName("last_name")
                                    .setType(Type.newBuilder().setCode(TypeCode.STRING).build())
                                    .build())
                            .addFields(
                                Field.newBuilder()
                                    .setName("nick_names")
                                    .setType(
                                        Type.newBuilder()
                                            .setCode(TypeCode.ARRAY)
                                            .setArrayElementType(
                                                Type.newBuilder().setCode(TypeCode.STRING).build())
                                            .build())
                                    .build())
                            .addFields(
                                Field.newBuilder()
                                    .setName("updated_at")
                                    .setType(Type.newBuilder().setCode(TypeCode.TIMESTAMP).build())
                                    .build())
                            .build())
                    .build())
            .addRows(
                ListValue.newBuilder()
                    .addValues(Value.newBuilder().setStringValue(singerId.toString()).build())
                    .addValues(Value.newBuilder().setBoolValue(true).build())
                    .addValues(
                        Value.newBuilder()
                            .setStringValue(OffsetDateTime.now(ZoneId.of("UTC")).toString())
                            .build())
                    .addValues(Value.newBuilder().setStringValue("Peter").build())
                    .addValues(Value.newBuilder().setStringValue("Peter Anderson").build())
                    .addValues(Value.newBuilder().setStringValue("Anderson").build())
                    .addValues(
                        Value.newBuilder()
                            .setListValue(
                                ListValue.newBuilder()
                                    .addValues(Value.newBuilder().setStringValue("chip").build())
                                    .addValues(Value.newBuilder().setStringValue("ash").build())
                                    .build())
                            .build())
                    .addValues(
                        Value.newBuilder()
                            .setStringValue(OffsetDateTime.now(ZoneId.of("UTC")).toString())
                            .build())
                    .build())
            .build();
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.newBuilder(
                    "select s1_0.id,s1_0.active,s1_0.created_at,s1_0.first_name,s1_0.full_name,s1_0.last_name,s1_0.nick_names,s1_0.updated_at from singer s1_0 limit @p1 offset @p2")
                .bind("p1")
                .to(20L)
                .bind("p2")
                .to(0L)
                .build(),
            singerResultSet));
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.newBuilder(
                    "select s1_0.id,s1_0.active,s1_0.created_at,s1_0.first_name,s1_0.full_name,s1_0.last_name,s1_0.nick_names,s1_0.updated_at from singer s1_0 where s1_0.id=@p1")
                .bind("p1")
                .to(singerId.toString())
                .build(),
            singerResultSet));
    mockSpanner.putPartialStatementResult(
        StatementResult.query(
            Statement.of(
                "select s1_0.id,s1_0.active,s1_0.created_at,s1_0.first_name,s1_0.full_name,s1_0.last_name,s1_0.nick_names,s1_0.updated_at from singer s1_0 where starts_with(s1_0.last_name,@p1)=true"),
            singerResultSet));
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.newBuilder(
                    "select s1_0.id,s1_0.active,s1_0.created_at,s1_0.first_name,s1_0.full_name,s1_0.last_name,s1_0.nick_names,s1_0.updated_at from singer @{FORCE_INDEX=idx_singer_active} s1_0 where s1_0.active=@p1")
                .bind("p1")
                .to(true)
                .build(),
            singerResultSet));

    // Add result for selecting albums.
    ResultSet albumResultSet =
        ResultSet.newBuilder()
            .setMetadata(
                ResultSetMetadata.newBuilder()
                    .setRowType(
                        StructType.newBuilder()
                            .addFields(
                                Field.newBuilder()
                                    .setName("id")
                                    .setType(Type.newBuilder().setCode(TypeCode.STRING).build())
                                    .build())
                            .addFields(
                                Field.newBuilder()
                                    .setName("cover_picture")
                                    .setType(Type.newBuilder().setCode(TypeCode.BYTES).build())
                                    .build())
                            .addFields(
                                Field.newBuilder()
                                    .setName("created_at")
                                    .setType(Type.newBuilder().setCode(TypeCode.TIMESTAMP).build())
                                    .build())
                            .addFields(
                                Field.newBuilder()
                                    .setName("marketing_budget")
                                    .setType(Type.newBuilder().setCode(TypeCode.NUMERIC).build())
                                    .build())
                            .addFields(
                                Field.newBuilder()
                                    .setName("release_date")
                                    .setType(Type.newBuilder().setCode(TypeCode.DATE).build())
                                    .build())
                            .addFields(
                                Field.newBuilder()
                                    .setName("singer_id")
                                    .setType(Type.newBuilder().setCode(TypeCode.STRING).build())
                                    .build())
                            .addFields(
                                Field.newBuilder()
                                    .setName("title")
                                    .setType(Type.newBuilder().setCode(TypeCode.STRING).build())
                                    .build())
                            .addFields(
                                Field.newBuilder()
                                    .setName("updated_at")
                                    .setType(Type.newBuilder().setCode(TypeCode.TIMESTAMP).build())
                                    .build())
                            .build())
                    .build())
            .addRows(
                ListValue.newBuilder()
                    .addValues(
                        Value.newBuilder().setStringValue(UUID.randomUUID().toString()).build())
                    .addValues(
                        Value.newBuilder()
                            .setStringValue(
                                Base64.getEncoder().encodeToString(new byte[] {1, 2, 3}))
                            .build())
                    .addValues(
                        Value.newBuilder()
                            .setStringValue(OffsetDateTime.now(ZoneId.of("UTC")).toString())
                            .build())
                    .addValues(Value.newBuilder().setStringValue("999.99").build())
                    .addValues(Value.newBuilder().setStringValue("2019-01-08").build())
                    .addValues(Value.newBuilder().setStringValue(singerId.toString()).build())
                    .addValues(Value.newBuilder().setStringValue("Hot Potato").build())
                    .addValues(
                        Value.newBuilder()
                            .setStringValue(OffsetDateTime.now(ZoneId.of("UTC")).toString())
                            .build())
                    .build())
            .build();
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.newBuilder(
                    "select a1_0.id,a1_0.cover_picture,a1_0.created_at,a1_0.marketing_budget,a1_0.release_date,a1_0.singer_id,a1_0.title,a1_0.updated_at from album a1_0 limit @p1 offset @p2")
                .bind("p1")
                .to(30L)
                .bind("p2")
                .to(0L)
                .build(),
            albumResultSet));
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.newBuilder(
                    "select a1_0.singer_id,a1_0.id,a1_0.cover_picture,a1_0.created_at,a1_0.marketing_budget,a1_0.release_date,a1_0.title,a1_0.updated_at from album a1_0 where a1_0.singer_id=@p1")
                .bind("p1")
                .to(singerId.toString())
                .build(),
            ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(
                            StructType.newBuilder()
                                .addFields(
                                    Field.newBuilder()
                                        .setName("singer_id")
                                        .setType(Type.newBuilder().setCode(TypeCode.STRING).build())
                                        .build())
                                .addFields(
                                    Field.newBuilder()
                                        .setName("id")
                                        .setType(Type.newBuilder().setCode(TypeCode.STRING).build())
                                        .build())
                                .addFields(
                                    Field.newBuilder()
                                        .setName("cover_picture")
                                        .setType(Type.newBuilder().setCode(TypeCode.BYTES).build())
                                        .build())
                                .addFields(
                                    Field.newBuilder()
                                        .setName("created_at")
                                        .setType(
                                            Type.newBuilder().setCode(TypeCode.TIMESTAMP).build())
                                        .build())
                                .addFields(
                                    Field.newBuilder()
                                        .setName("marketing_budget")
                                        .setType(
                                            Type.newBuilder().setCode(TypeCode.NUMERIC).build())
                                        .build())
                                .addFields(
                                    Field.newBuilder()
                                        .setName("release_date")
                                        .setType(Type.newBuilder().setCode(TypeCode.DATE).build())
                                        .build())
                                .addFields(
                                    Field.newBuilder()
                                        .setName("title")
                                        .setType(Type.newBuilder().setCode(TypeCode.STRING).build())
                                        .build())
                                .addFields(
                                    Field.newBuilder()
                                        .setName("updated_at")
                                        .setType(
                                            Type.newBuilder().setCode(TypeCode.TIMESTAMP).build())
                                        .build())
                                .build())
                        .build())
                .addRows(
                    ListValue.newBuilder()
                        .addValues(Value.newBuilder().setStringValue(singerId.toString()).build())
                        .addValues(
                            Value.newBuilder().setStringValue(UUID.randomUUID().toString()).build())
                        .addValues(
                            Value.newBuilder()
                                .setStringValue(
                                    Base64.getEncoder().encodeToString(new byte[] {1, 2, 3}))
                                .build())
                        .addValues(
                            Value.newBuilder()
                                .setStringValue(OffsetDateTime.now(ZoneId.of("UTC")).toString())
                                .build())
                        .addValues(Value.newBuilder().setStringValue("999.99").build())
                        .addValues(Value.newBuilder().setStringValue("2019-01-08").build())
                        .addValues(Value.newBuilder().setStringValue("Hot Potato").build())
                        .addValues(
                            Value.newBuilder()
                                .setStringValue(OffsetDateTime.now(ZoneId.of("UTC")).toString())
                                .build())
                        .build())
                .build()));
    // Add results for selecting tracks.
    mockSpanner.putPartialStatementResult(
        StatementResult.query(
            Statement.of(
                "select t1_0.id,t1_0.track_number,t1_0.created_at,t1_0.sample_rate,t1_0.title,t1_0.updated_at from track t1_0 where t1_0.id=@p1"),
            empty()));

    // Add results for selecting venues.
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of(
                "select v1_0.id,v1_0.created_at,v1_0.description,v1_0.name,v1_0.updated_at from venue v1_0"),
            ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(
                            StructType.newBuilder()
                                .addFields(
                                    Field.newBuilder()
                                        .setName("id")
                                        .setType(Type.newBuilder().setCode(TypeCode.STRING).build())
                                        .build())
                                .addFields(
                                    Field.newBuilder()
                                        .setName("created_at")
                                        .setType(
                                            Type.newBuilder().setCode(TypeCode.TIMESTAMP).build())
                                        .build())
                                .addFields(
                                    Field.newBuilder()
                                        .setName("description")
                                        .setType(Type.newBuilder().setCode(TypeCode.JSON).build())
                                        .build())
                                .addFields(
                                    Field.newBuilder()
                                        .setName("name")
                                        .setType(Type.newBuilder().setCode(TypeCode.STRING).build())
                                        .build())
                                .addFields(
                                    Field.newBuilder()
                                        .setName("updated_at")
                                        .setType(
                                            Type.newBuilder().setCode(TypeCode.TIMESTAMP).build())
                                        .build())
                                .build())
                        .build())
                .addRows(
                    ListValue.newBuilder()
                        .addValues(
                            Value.newBuilder().setStringValue(UUID.randomUUID().toString()).build())
                        .addValues(
                            Value.newBuilder()
                                .setStringValue(OffsetDateTime.now(ZoneId.of("UTC")).toString())
                                .build())
                        .addValues(
                            Value.newBuilder()
                                .setStringValue("{\"type\": \"stadium\", \"capacity\": 100}")
                                .build())
                        .addValues(Value.newBuilder().setStringValue("Stadium").build())
                        .addValues(
                            Value.newBuilder()
                                .setStringValue(OffsetDateTime.now(ZoneId.of("UTC")).toString())
                                .build())
                        .build())
                .build()));
    // Add results for selecting concerts.
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.newBuilder(
                    "select c1_0.singer_id,c1_0.id,c1_0.created_at,c1_0.end_time,c1_0.name,c1_0.start_time,c1_0.updated_at,c1_0.venue_id from concert c1_0 where c1_0.singer_id=@p1")
                .bind("p1")
                .to(singerId.toString())
                .build(),
            empty()));
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of("select current_timestamp"),
            ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(
                            StructType.newBuilder()
                                .addFields(
                                    Field.newBuilder()
                                        .setName("current_timestamp")
                                        .setType(
                                            Type.newBuilder().setCode(TypeCode.TIMESTAMP).build())
                                        .build())
                                .build())
                        .build())
                .addRows(
                    ListValue.newBuilder()
                        .addValues(
                            Value.newBuilder()
                                .setStringValue(OffsetDateTime.now(ZoneId.of("UTC")).toString())
                                .build())
                        .build())
                .build()));

    // Add results for selecting albums.
    String selectAlbumsSql =
        "select a1_0.id,a1_0.cover_picture,a1_0.created_at,a1_0.marketing_budget,a1_0.release_date,a1_0.singer_id,a1_0.title,a1_0.updated_at from album a1_0 where a1_0.title=@p1";
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.newBuilder(selectAlbumsSql).bind("p1").to("Foo").build(), empty()));
  }

  static ResultSet generateSequenceResultSet(long startInclusive, long endInclusive) {
    return ResultSet.newBuilder()
        .setMetadata(
            ResultSetMetadata.newBuilder()
                .setRowType(
                    StructType.newBuilder()
                        .addFields(
                            Field.newBuilder()
                                .setType(Type.newBuilder().setCode(TypeCode.INT64).build())
                                .setName("n")
                                .build())
                        .build())
                .build())
        .addAllRows(
            LongStream.rangeClosed(startInclusive, endInclusive)
                .map(Long::reverse)
                .mapToObj(
                    id ->
                        ListValue.newBuilder()
                            .addValues(
                                Value.newBuilder().setStringValue(String.valueOf(id)).build())
                            .build())
                .collect(Collectors.toList()))
        .build();
  }

  @Test
  public void testRunApplication() {
    SpannerOptionsHelper.resetActiveTracingFramework();
    GlobalOpenTelemetry.resetForTest();

    System.setProperty("spanner.emulator", "false");
    System.setProperty("spanner.host", "//localhost:" + getPort());
    System.setProperty("spanner.connectionProperties", ";usePlainText=true");
    // Enable automatic tagging of transactions that do not already have a tag.
    System.setProperty("spanner.auto_tag_transactions", "true");
    SampleApplication.main(new String[] {});

    assertEquals(1, mockDatabaseAdmin.getRequests().size());
    UpdateDatabaseDdlRequest ddlRequest =
        (UpdateDatabaseDdlRequest) mockDatabaseAdmin.getRequests().get(0);
    assertEquals(13, ddlRequest.getStatementsCount());
    int index = -1;
    assertEquals(
        "create sequence ticket_sale_seq options(sequence_kind=\"bit_reversed_positive\", start_with_counter=50000, skip_range_min=1, skip_range_max=1000)",
        ddlRequest.getStatements(++index));
    assertEquals(
        "create table album (id string(36) not null,created_at timestamp,updated_at timestamp,cover_picture bytes(1000000),marketing_budget numeric,release_date date,title string(200) not null,singer_id string(36) not null) PRIMARY KEY (id)",
        ddlRequest.getStatements(++index));
    assertEquals(
        "create table concert (id string(36) not null,created_at timestamp,updated_at timestamp,end_time timestamp,name string(255),start_time timestamp,singer_id string(36) not null,venue_id string(36) not null) PRIMARY KEY (id)",
        ddlRequest.getStatements(++index));
    assertEquals(
        "create table singer (id string(36) not null,created_at timestamp,updated_at timestamp,active bool default (true),first_name string(100),full_name STRING(300) AS (\n"
            + "CASE WHEN first_name IS NULL THEN last_name\n"
            + "     WHEN last_name IS NULL THEN first_name\n"
            + "     ELSE first_name || ' ' || last_name\n"
            + "END) STORED,last_name string(200),nick_names ARRAY<STRING(255)>) PRIMARY KEY (id)",
        ddlRequest.getStatements(++index));
    assertEquals(
        "create table ticket_sale (id int64 not null,created_at timestamp,updated_at timestamp,customer_name string(255),price numeric,seats ARRAY<STRING(MAX)>,concert_id string(36) not null) PRIMARY KEY (id)",
        ddlRequest.getStatements(++index));
    assertEquals(
        "create table track (id string(36) not null,track_number int64 not null,created_at timestamp,updated_at timestamp,sample_rate float64,title string(100) not null) PRIMARY KEY (id,track_number), INTERLEAVE IN PARENT album ON DELETE CASCADE",
        ddlRequest.getStatements(++index));
    assertEquals(
        "create table venue (id string(36) not null,created_at timestamp,updated_at timestamp,description json,name string(255)) PRIMARY KEY (id)",
        ddlRequest.getStatements(++index));
    assertEquals(
        "create index idx_singer_active on singer (active)", ddlRequest.getStatements(++index));
    assertEquals(
        "alter table album add constraint FK1imrvvcsp892ggu1tlcbhavjm foreign key (singer_id) references singer (id)",
        ddlRequest.getStatements(++index));
    assertEquals(
        "alter table concert add constraint FKcixl9gtki9i7pvjabe6bcbpem foreign key (singer_id) references singer (id)",
        ddlRequest.getStatements(++index));
    assertEquals(
        "alter table concert add constraint FKllw6cymtwrumecg1bcnsubw56 foreign key (venue_id) references venue (id)",
        ddlRequest.getStatements(++index));
    assertEquals(
        "alter table ticket_sale add constraint FKqd5hyt9v2omxqhuf2gukkiw0n foreign key (concert_id) references concert (id)",
        ddlRequest.getStatements(++index));
    assertEquals(
        "alter table track add constraint FKhwcw1xkgnlrd4n15sfe8p8eto foreign key (id) references album (id)",
        ddlRequest.getStatements(++index));
    assertEquals(ddlRequest.getStatementsCount() - 1, index);

    assertEquals(32, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    assertEquals(2, mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class));
    assertEquals(4, mockSpanner.countRequestsOfType(CommitRequest.class));

    // Verify that we receive a transaction tag for the generateRandomData() method.
    assertEquals(
        1,
        mockSpanner.getRequestsOfType(ExecuteBatchDmlRequest.class).stream()
            .filter(
                request ->
                    request.getRequestOptions().getTransactionTag().equals("generate_random_data"))
            .count());
    assertEquals(
        1,
        mockSpanner.getRequestsOfType(CommitRequest.class).stream()
            .filter(
                request ->
                    request.getRequestOptions().getTransactionTag().equals("generate_random_data"))
            .count());
    CommitRequest commitRequest =
        mockSpanner.getRequestsOfType(CommitRequest.class).stream()
            .filter(
                request ->
                    request.getRequestOptions().getTransactionTag().equals("generate_random_data"))
            .findAny()
            .orElseThrow();
    assertEquals(
        Duration.newBuilder()
            .setNanos((int) TimeUnit.NANOSECONDS.convert(50, TimeUnit.MILLISECONDS))
            .build(),
        commitRequest.getMaxCommitDelay());
    // Also verify that we get the auto-generated transaction tags.
    assertEquals(
        2,
        mockSpanner.getRequestsOfType(ExecuteBatchDmlRequest.class).stream()
            .filter(
                request -> !Strings.isNullOrEmpty(request.getRequestOptions().getTransactionTag()))
            .count());
    assertEquals(
        4,
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
            .filter(
                request ->
                    request
                        .getRequestOptions()
                        .getTransactionTag()
                        .equals("service_BatchService_deleteAllData"))
            .count());
    assertEquals(
        3,
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
            .filter(
                request ->
                    request
                        .getRequestOptions()
                        .getRequestTag()
                        .equals("search_singers_by_last_name_starts_with"))
            .count());
    // Verify that the last commit request contains one mutation with 10 inserts.
    CommitRequest lastCommitRequest = mockSpanner.getRequestsOfType(CommitRequest.class).get(3);
    assertEquals(1, lastCommitRequest.getMutationsCount());
    assertEquals(10, lastCommitRequest.getMutations(0).getInsertOrUpdate().getValuesCount());
  }

  private static void addDdlResponseToSpannerAdmin() {
    mockDatabaseAdmin.addResponse(
        Operation.newBuilder()
            .setDone(true)
            .setResponse(Any.pack(Empty.getDefaultInstance()))
            .setMetadata(Any.pack(UpdateDatabaseDdlMetadata.getDefaultInstance()))
            .build());
  }
}
