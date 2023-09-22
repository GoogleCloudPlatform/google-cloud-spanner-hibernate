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
import org.junit.Before;
import org.junit.Test;

/**
 * Verifies that the correct database schema is being generated, and that the schema generation uses
 * a DDL batch.
 */
public class SchemaGenerationMockServerTest extends AbstractSchemaGenerationMockServerTest {

  /**
   * Set up empty mocked results for schema queries.
   */
  @Before
  public void setupSchemaQueryResults() {
    mockSpanner.putStatementResult(
        StatementResult.query(
            GET_TABLES_STATEMENT,
            ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(StructType.newBuilder().build())
                        .build())
                .build()));
    mockSpanner.putStatementResult(
        StatementResult.query(
            GET_INDEXES_STATEMENT,
            ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(StructType.newBuilder().build())
                        .build())
                .build()));
    mockSpanner.putStatementResult(StatementResult.query(GET_COLUMNS_STATEMENT, ResultSet
        .newBuilder()
        .setMetadata(GET_COLUMNS_METADATA)
        .build()));
  }

  @Test
  public void testGenerateSchema() {
    for (String hbm2Ddl : new String[]{"create-only", "update", "create"}) {
      mockDatabaseAdmin.getRequests().clear();
      addDdlResponseToSpannerAdmin();

      //noinspection EmptyTryBlock
      try (SessionFactory ignore =
          createTestHibernateConfig(
              ImmutableList.of(Singer.class, Invoice.class, Customer.class, Account.class),
              ImmutableMap.of("hibernate.hbm2ddl.auto", hbm2Ddl))
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
          "create table Account (id INT64 not null,amount NUMERIC,name STRING(255)) PRIMARY KEY (id)",
          request.getStatements(++index));
      assertEquals(
          "create table Customer (customerId INT64 not null,name STRING(255)) PRIMARY KEY (customerId)",
          request.getStatements(++index));
      assertEquals(
          "create table customerId (next_val INT64) PRIMARY KEY ()",
          request.getStatements(++index));
      assertEquals(
          "create table Invoice (invoiceId INT64 not null,number STRING(255),customer_customerId INT64) PRIMARY KEY (invoiceId)",
          request.getStatements(++index));
      assertEquals(
          "create table invoiceId (next_val INT64) PRIMARY KEY ()", request.getStatements(++index));
      assertEquals(
          "create table Singer (id INT64 not null) PRIMARY KEY (id)",
          request.getStatements(++index));
      assertEquals(
          "create table singerId (next_val INT64) PRIMARY KEY ()", request.getStatements(++index));
      assertEquals(
          "alter table Invoice add constraint fk_invoice_customer foreign key (customer_customerId) references Customer (customerId) on delete cascade",
          request.getStatements(++index));
    }
  }
  
  @Test
  public void testDropEmptySchema() {
    addDdlResponseToSpannerAdmin();

    //noinspection EmptyTryBlock
    try (SessionFactory ignore =
        createTestHibernateConfig(
            ImmutableList.of(Singer.class, Invoice.class, Customer.class, Account.class),
            ImmutableMap.of("hibernate.hbm2ddl.auto", "drop"))
            .buildSessionFactory()) {
      // do nothing, just generate the schema.
    }

    // Check the DDL statements that were generated.
    List<UpdateDatabaseDdlRequest> requests =
        mockDatabaseAdmin.getRequests().stream()
            .filter(request -> request instanceof UpdateDatabaseDdlRequest)
            .map(request -> (UpdateDatabaseDdlRequest) request)
            .collect(Collectors.toList());
    assertEquals(0, requests.size());
  }

  @Test
  public void testDropExistingSchema() {
    mockSpanner.putStatementResult(
        StatementResult.query(GET_TABLES_STATEMENT, ResultSet.newBuilder()
            .setMetadata(GET_TABLES_METADATA)
            .addRows(createTableRow("Account"))
            .addRows(createTableRow("Customer"))
            .addRows(createTableRow("customerId"))
            .addRows(createTableRow("Invoice"))
            .addRows(createTableRow("invoiceId"))
            .addRows(createTableRow("Singer"))
            .addRows(createTableRow("singerId"))
            .build()));
    mockSpanner.putStatementResult(StatementResult.query(GET_FOREIGN_KEYS_STATEMENT.toBuilder()
        .bind("p1").to("%")
        .bind("p2").to("%")
        .bind("p3").to("INVOICE")
        .build(), ResultSet.newBuilder()
        .setMetadata(GET_FOREIGN_KEYS_METADATA)
        .addRows(createForeignKeyRow("", "Customer", "customerId",
            "", "Invoice", "customer_customerId", 1, 1, "fk_invoice_customer"))
        .build()));

    addDdlResponseToSpannerAdmin();

    //noinspection EmptyTryBlock
    try (SessionFactory ignore =
        createTestHibernateConfig(
            ImmutableList.of(Singer.class, Invoice.class, Customer.class, Account.class),
            ImmutableMap.of("hibernate.hbm2ddl.auto", "drop"))
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

    assertEquals("alter table Invoice drop constraint fk_invoice_customer",
        request.getStatements(++index));
    assertEquals("drop table Account", request.getStatements(++index));
    assertEquals("drop table Customer", request.getStatements(++index));
    assertEquals("drop table customerId", request.getStatements(++index));
    assertEquals("drop table Invoice", request.getStatements(++index));
    assertEquals("drop table invoiceId", request.getStatements(++index));
    assertEquals("drop table Singer", request.getStatements(++index));
    assertEquals("drop table singerId", request.getStatements(++index));
  }
  
  @Test
  public void testGenerateEmployeeSchema() {
    for (String hbm2Ddl : new String[]{"create-only", "update", "create"}) {
      mockDatabaseAdmin.getRequests().clear();
      addDdlResponseToSpannerAdmin();

      //noinspection EmptyTryBlock
      try (SessionFactory ignore =
          createTestHibernateConfig(
              ImmutableList.of(Employee.class),
              ImmutableMap.of("hibernate.hbm2ddl.auto", hbm2Ddl))
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
          "create table Employee (id INT64 not null,name STRING(255),manager_id INT64) PRIMARY KEY (id)",
          request.getStatements(++index));
      assertEquals(
          "create table hibernate_sequence (next_val INT64) PRIMARY KEY ()",
          request.getStatements(++index));
      assertEquals("create index name_index on Employee (name)", request.getStatements(++index));
      assertEquals(
          "alter table Employee add constraint FKiralam2duuhr33k8a10aoc2t6 foreign key (manager_id) references Employee (id)",
          request.getStatements(++index));
    }
  }

  @Test
  public void testGenerateAirportSchema() {
    for (String hbm2Ddl : new String[]{"create-only", "update", "create"}) {
      mockDatabaseAdmin.getRequests().clear();
      addDdlResponseToSpannerAdmin();

      //noinspection EmptyTryBlock
      try (SessionFactory ignore =
          createTestHibernateConfig(
              ImmutableList.of(Airplane.class, Airport.class),
              ImmutableMap.of("hibernate.hbm2ddl.auto", hbm2Ddl))
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
          "create table Airplane (id STRING(255) not null,modelName STRING(255)) PRIMARY KEY (id)",
          request.getStatements(++index));
      assertEquals(
          "create table Airport (id STRING(255) not null) PRIMARY KEY (id)",
          request.getStatements(++index));
      assertEquals(
          "create table Airport_Airplane (Airport_id STRING(255) not null,airplanes_id STRING(255) not null) PRIMARY KEY (Airport_id,airplanes_id)",
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
  }

  @Test
  public void testGenerateParentChildSchema() {
    for (String hbm2Ddl : new String[]{"create-only", "update", "create"}) {
      mockDatabaseAdmin.getRequests().clear();
      addDdlResponseToSpannerAdmin();

      //noinspection EmptyTryBlock
      try (SessionFactory ignore =
          createTestHibernateConfig(
              ImmutableList.of(GrandParent.class, Parent.class, Child.class),
              ImmutableMap.of("hibernate.hbm2ddl.auto", hbm2Ddl))
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
          "create table GrandParent (grandParentId INT64 not null,name STRING(255)) PRIMARY KEY (grandParentId)",
          request.getStatements(++index));
      assertEquals(
          "create table Parent (grandParentId INT64 not null,parentId INT64 not null,name STRING(255)) "
              + "PRIMARY KEY (grandParentId,parentId), INTERLEAVE IN PARENT GrandParent",
          request.getStatements(++index));
      assertEquals(
          "create table Child (childId INT64 not null,grandParentId INT64 not null,parentId INT64 not null,name STRING(255)) "
              + "PRIMARY KEY (grandParentId,parentId,childId), INTERLEAVE IN PARENT Parent",
          request.getStatements(++index));
      assertEquals(
          "create table hibernate_sequence (next_val INT64) PRIMARY KEY ()",
          request.getStatements(++index));
    }
  }

  @Test
  public void testGenerateTestEntitySchema() {
    for (String hbm2Ddl : new String[]{"create-only", "update", "create"}) {
      mockDatabaseAdmin.getRequests().clear();
      addDdlResponseToSpannerAdmin();

      //noinspection EmptyTryBlock
      try (SessionFactory ignore =
          createTestHibernateConfig(
              ImmutableList.of(TestEntity.class, SubTestEntity.class),
              ImmutableMap.of("hibernate.hbm2ddl.auto", hbm2Ddl))
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
              + "`TestEntity_ID1` INT64 not null,"
              + "`TestEntity_id2` STRING(255) not null,"
              + "stringList STRING(255)) "
              + "PRIMARY KEY (`TestEntity_ID1`,`TestEntity_id2`,stringList)",
          request.getStatements(++index));
      assertEquals(
          "create table SubTestEntity (id STRING(255) not null,id1 INT64,id2 STRING(255)) PRIMARY KEY (id)",
          request.getStatements(++index));
      assertEquals(
          "create table `test_table` ("
              + "`ID1` INT64 not null,id2 STRING(255) not null,"
              + "`boolColumn` BOOL,"
              + "longVal INT64 not null,"
              + "stringVal STRING(255)) "
              + "PRIMARY KEY (`ID1`,id2)",
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
}
