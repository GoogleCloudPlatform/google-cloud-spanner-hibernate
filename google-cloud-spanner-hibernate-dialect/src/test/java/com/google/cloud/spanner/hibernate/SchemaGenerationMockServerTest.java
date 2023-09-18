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

import static org.junit.Assert.assertEquals;

import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.hibernate.entities.Account;
import com.google.cloud.spanner.hibernate.entities.Airplane;
import com.google.cloud.spanner.hibernate.entities.Airport;
import com.google.cloud.spanner.hibernate.entities.Child;
import com.google.cloud.spanner.hibernate.entities.Customer;
import com.google.cloud.spanner.hibernate.entities.Employee;
import com.google.cloud.spanner.hibernate.entities.GrandParent;
import com.google.cloud.spanner.hibernate.entities.Invoice;
import com.google.cloud.spanner.hibernate.entities.Parent;
import com.google.cloud.spanner.hibernate.entities.Singer;
import com.google.cloud.spanner.hibernate.entities.SubTestEntity;
import com.google.cloud.spanner.hibernate.entities.TestEntity;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlRequest;
import com.google.spanner.v1.ResultSet;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.SessionFactory;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies that the correct database schema is being generated, and that the schema generation uses
 * a DDL batch.
 */
public class SchemaGenerationMockServerTest extends AbstractMockSpannerServerTest {

  /** Set up empty mocked results for schema queries. */
  @BeforeClass
  public static void setupSchemaQueryResults() {
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
            ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(StructType.newBuilder().build())
                        .build())
                .build()));
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
            ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(StructType.newBuilder().build())
                        .build())
                .build()));
  }

  @Test
  public void testGenerateSchema() {
    addDdlResponseToSpannerAdmin();

    //noinspection EmptyTryBlock
    try (SessionFactory ignore =
        createTestHibernateConfig(
                ImmutableList.of(Singer.class, Invoice.class, Customer.class, Account.class),
                ImmutableMap.of("hibernate.hbm2ddl.auto", "create-only"))
            .buildSessionFactory()) {
      // do nothing, just generate the schema.
    }

    // Check the DDL statements that were generated.
    List<UpdateDatabaseDdlRequest> requests =
        mockDatabaseAdmin.getRequests().stream()
            .filter(request -> request instanceof UpdateDatabaseDdlRequest)
            .map(request -> (UpdateDatabaseDdlRequest) request)
            .collect(Collectors.toList());
    assertEquals(1, requests.size());
    UpdateDatabaseDdlRequest request = requests.get(0);
    assertEquals(8, request.getStatementsCount());

    int index = -1;

    assertEquals(
        "create table Account (amount float64,id int64 not null,name string(255)) PRIMARY KEY (id)",
        request.getStatements(++index));
    assertEquals(
        "create table Customer (customerId int64 not null,name string(255)) PRIMARY KEY (customerId)",
        request.getStatements(++index));
    assertEquals(
        "create table customerId (next_val int64) PRIMARY KEY ()", request.getStatements(++index));
    assertEquals(
        "create table Invoice (customer_customerId int64,invoiceId int64 not null,number string(255)) PRIMARY KEY (invoiceId)",
        request.getStatements(++index));
    assertEquals(
        "create table invoiceId (next_val int64) PRIMARY KEY ()", request.getStatements(++index));
    assertEquals(
        "create table Singer (id int64 not null) PRIMARY KEY (id)", request.getStatements(++index));
    assertEquals(
        "create table singerId (next_val int64) PRIMARY KEY ()", request.getStatements(++index));
    assertEquals(
        "alter table Invoice add constraint fk_invoice_customer foreign key (customer_customerId) references Customer (customerId) on delete cascade",
        request.getStatements(++index));
  }

  @Test
  public void testGenerateEmployeeSchema() {
    addDdlResponseToSpannerAdmin();

    //noinspection EmptyTryBlock
    try (SessionFactory ignore =
        createTestHibernateConfig(
                ImmutableList.of(Employee.class),
                ImmutableMap.of("hibernate.hbm2ddl.auto", "create-only"))
            .buildSessionFactory()) {
      // do nothing, just generate the schema.
    }

    // Check the DDL statements that were generated.
    List<UpdateDatabaseDdlRequest> requests =
        mockDatabaseAdmin.getRequests().stream()
            .filter(request -> request instanceof UpdateDatabaseDdlRequest)
            .map(request -> (UpdateDatabaseDdlRequest) request)
            .collect(Collectors.toList());
    assertEquals(1, requests.size());
    UpdateDatabaseDdlRequest request = requests.get(0);
    assertEquals(4, request.getStatementsCount());

    int index = -1;

    assertEquals(
        "create table Employee (id int64 not null,manager_id int64,name string(255)) PRIMARY KEY (id)",
        request.getStatements(++index));
    assertEquals(
        "create table Employee_SEQ (next_val int64) PRIMARY KEY ()",
        request.getStatements(++index));
    assertEquals("create index name_index on Employee (name)", request.getStatements(++index));
    assertEquals(
        "alter table Employee add constraint FKiralam2duuhr33k8a10aoc2t6 foreign key (manager_id) references Employee (id)",
        request.getStatements(++index));
  }

  @Test
  public void testGenerateAirportSchema() {
    addDdlResponseToSpannerAdmin();

    //noinspection EmptyTryBlock
    try (SessionFactory ignore =
        createTestHibernateConfig(
                ImmutableList.of(Airplane.class, Airport.class),
                ImmutableMap.of("hibernate.hbm2ddl.auto", "create-only"))
            .buildSessionFactory()) {
      // do nothing, just generate the schema.
    }

    // Check the DDL statements that were generated.
    List<UpdateDatabaseDdlRequest> requests =
        mockDatabaseAdmin.getRequests().stream()
            .filter(request -> request instanceof UpdateDatabaseDdlRequest)
            .map(request -> (UpdateDatabaseDdlRequest) request)
            .collect(Collectors.toList());
    assertEquals(1, requests.size());
    UpdateDatabaseDdlRequest request = requests.get(0);
    assertEquals(7, request.getStatementsCount());

    int index = -1;

    assertEquals(
        "create table Airplane (id string(36) not null,modelName string(255)) PRIMARY KEY (id)",
        request.getStatements(++index));
    assertEquals(
        "create table Airport (id string(36) not null) PRIMARY KEY (id)",
        request.getStatements(++index));
    assertEquals(
        "create table Airport_Airplane (Airport_id string(36) not null,airplanes_id string(36) not null) PRIMARY KEY (Airport_id,airplanes_id)",
        request.getStatements(++index));

    assertEquals(
        "create unique index UK_gc568wb30sampsuirwne5jqgh on Airplane (modelName)",
        request.getStatements(++index));
    assertEquals(
        "create unique index UK_em0lqvwoqdwt29x0b0r010be on Airport_Airplane (airplanes_id)",
        request.getStatements(++index));
    assertEquals(
        "alter table Airport_Airplane add constraint FKkn0enwaxbwk7csf52x0eps73d foreign key (airplanes_id) references Airplane (id)",
        request.getStatements(++index));
    assertEquals(
        "alter table Airport_Airplane add constraint FKh186t28ublke8o13fo4ppogs7 foreign key (Airport_id) references Airport (id)",
        request.getStatements(++index));
  }

  @Test
  public void testGenerateParentChildSchema() {
    addDdlResponseToSpannerAdmin();

    //noinspection EmptyTryBlock
    try (SessionFactory ignore =
        createTestHibernateConfig(
                ImmutableList.of(GrandParent.class, Parent.class, Child.class),
                ImmutableMap.of("hibernate.hbm2ddl.auto", "create-only"))
            .buildSessionFactory()) {
      // do nothing, just generate the schema.
    }

    // Check the DDL statements that were generated.
    List<UpdateDatabaseDdlRequest> requests =
        mockDatabaseAdmin.getRequests().stream()
            .filter(request -> request instanceof UpdateDatabaseDdlRequest)
            .map(request -> (UpdateDatabaseDdlRequest) request)
            .collect(Collectors.toList());
    assertEquals(1, requests.size());
    UpdateDatabaseDdlRequest request = requests.get(0);
    assertEquals(4, request.getStatementsCount());

    int index = -1;

    assertEquals(
        "create table GrandParent (grandParentId int64 not null,name string(255)) PRIMARY KEY (grandParentId)",
        request.getStatements(++index));
    assertEquals(
        "create table Parent (grandParentId int64 not null,parentId int64 not null,name string(255)) "
            + "PRIMARY KEY (grandParentId,parentId), INTERLEAVE IN PARENT GrandParent",
        request.getStatements(++index));
    assertEquals(
        "create table Child (childId int64 not null,grandParentId int64 not null,parentId int64 not null,name string(255)) "
            + "PRIMARY KEY (grandParentId,parentId,childId), INTERLEAVE IN PARENT Parent",
        request.getStatements(++index));
    assertEquals(
        "create table GrandParent_SEQ (next_val int64) PRIMARY KEY ()",
        request.getStatements(++index));
  }

  @Test
  public void testGenerateTestEntitySchema() {
    addDdlResponseToSpannerAdmin();

    //noinspection EmptyTryBlock
    try (SessionFactory ignore =
        createTestHibernateConfig(
                ImmutableList.of(TestEntity.class, SubTestEntity.class),
                ImmutableMap.of("hibernate.hbm2ddl.auto", "create-only"))
            .buildSessionFactory()) {
      // do nothing, just generate the schema.
    }

    // Check the DDL statements that were generated.
    List<UpdateDatabaseDdlRequest> requests =
        mockDatabaseAdmin.getRequests().stream()
            .filter(request -> request instanceof UpdateDatabaseDdlRequest)
            .map(request -> (UpdateDatabaseDdlRequest) request)
            .collect(Collectors.toList());
    assertEquals(1, requests.size());
    UpdateDatabaseDdlRequest request = requests.get(0);
    assertEquals(5, request.getStatementsCount());

    int index = -1;

    assertEquals(
        "create table `TestEntity_stringList` ("
            + "`TestEntity_ID1` int64 not null,"
            + "`TestEntity_id2` string(255) not null,"
            + "stringList string(255)) "
            + "PRIMARY KEY (`TestEntity_ID1`,`TestEntity_id2`,stringList)",
        request.getStatements(++index));
    assertEquals(
        "create table SubTestEntity (id1 int64,id string(255) not null,id2 string(255)) PRIMARY KEY (id)",
        request.getStatements(++index));
    assertEquals(
        "create table `test_table` (" 
            + "`boolColumn` bool," 
            + "`ID1` int64 not null," 
            + "longVal int64 not null," 
            + "id2 string(255) not null" 
            + ",stringVal string(255)) PRIMARY KEY (`ID1`,id2)",
        request.getStatements(++index));
    assertEquals(
        "alter table `TestEntity_stringList` add constraint FK2is6fwy3079dmfhjot09x5och "
            + "foreign key (`TestEntity_ID1`, `TestEntity_id2`) references `test_table` (`ID1`, id2)",
        request.getStatements(++index));
    assertEquals(
        "alter table SubTestEntity add constraint FK45l9js1jvci3yy21exuclnku0 "
            + "foreign key (id1, id2) references `test_table` (`ID1`, id2)",
        request.getStatements(++index));
  }
}
