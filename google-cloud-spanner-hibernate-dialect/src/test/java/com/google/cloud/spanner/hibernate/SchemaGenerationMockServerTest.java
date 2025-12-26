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
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.hibernate.entities.Account;
import com.google.cloud.spanner.hibernate.entities.Airplane;
import com.google.cloud.spanner.hibernate.entities.Airport;
import com.google.cloud.spanner.hibernate.entities.Album;
import com.google.cloud.spanner.hibernate.entities.AutoIdEntity;
import com.google.cloud.spanner.hibernate.entities.Child;
import com.google.cloud.spanner.hibernate.entities.Customer;
import com.google.cloud.spanner.hibernate.entities.Employee;
import com.google.cloud.spanner.hibernate.entities.GrandParent;
import com.google.cloud.spanner.hibernate.entities.IdentityEntity;
import com.google.cloud.spanner.hibernate.entities.Invoice;
import com.google.cloud.spanner.hibernate.entities.LegacySequenceEntity;
import com.google.cloud.spanner.hibernate.entities.Parent;
import com.google.cloud.spanner.hibernate.entities.PooledBitReversedSequenceEntity;
import com.google.cloud.spanner.hibernate.entities.PooledSequenceEntity;
import com.google.cloud.spanner.hibernate.entities.SequenceEntity;
import com.google.cloud.spanner.hibernate.entities.Singer;
import com.google.cloud.spanner.hibernate.entities.SubTestEntity;
import com.google.cloud.spanner.hibernate.entities.TestEntity;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ResultSet;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.RollbackRequest;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.Type;
import com.google.spanner.v1.TypeCode;
import java.sql.Types;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Environment;
import org.junit.Before;
import org.junit.Test;

/**
 * Verifies that the correct database schema is being generated, and that the schema generation uses
 * a DDL batch.
 */
public class SchemaGenerationMockServerTest extends AbstractSchemaGenerationMockServerTest {

  /** Set up empty mocked results for schema queries. */
  @Before
  public void setupSchemaQueryResults() {
    // Default wildcard results
    mockSpanner.putStatementResult(
        StatementResult.query(
            GET_TABLES_STATEMENT, ResultSet.newBuilder().setMetadata(GET_TABLES_METADATA).build()));
    mockSpanner.putStatementResult(
        StatementResult.query(
            GET_INDEXES_STATEMENT,
            ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(StructType.newBuilder().build())
                        .build())
                .build()));
    mockSpanner.putStatementResult(
        StatementResult.query(
            GET_SEQUENCES_STATEMENT,
            ResultSet.newBuilder().setMetadata(GET_SEQUENCES_METADATA).build()));
    mockSpanner.putStatementResult(
        StatementResult.query(
            GET_COLUMNS_STATEMENT,
            ResultSet.newBuilder().setMetadata(GET_COLUMNS_METADATA).build()));

    // Explicit empty schema ("") results for SpannerDatabaseInfo compatibility
    mockSpanner.putStatementResult(
        StatementResult.query(
            GET_TABLES_STATEMENT.toBuilder().bind("p2").to("").build(),
            ResultSet.newBuilder().setMetadata(GET_TABLES_METADATA).build()));
    mockSpanner.putStatementResult(
        StatementResult.query(
            GET_INDEXES_STATEMENT.toBuilder().bind("p2").to("").build(),
            ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(StructType.newBuilder().build())
                        .build())
                .build()));
    mockSpanner.putStatementResult(
        StatementResult.query(
            GET_COLUMNS_STATEMENT.toBuilder().bind("p2").to("").build(),
            ResultSet.newBuilder().setMetadata(GET_COLUMNS_METADATA).build()));
  }

  @Test
  public void testGenerateSchema() {
    for (String hbm2Ddl : new String[] {"create-only", "update", "create"}) {
      mockDatabaseAdmin.getRequests().clear();
      // register response for extra batch request dropping sequences
      if (hbm2Ddl.equals("create")) {
        addDdlResponseToSpannerAdmin();
      }
      addDdlResponseToSpannerAdmin();
      //noinspection EmptyTryBlock
      try (SessionFactory ignore =
          createTestHibernateConfig(
                  ImmutableList.of(
                      Singer.class, Album.class, Invoice.class, Customer.class, Account.class),
                  ImmutableMap.of("hibernate.hbm2ddl.auto", hbm2Ddl, "hibernate.show_sql", "true"))
              .buildSessionFactory()) {
        // do nothing, just generate the schema.
      }

      // Check the DDL statements that were generated.
      List<UpdateDatabaseDdlRequest> requests =
          mockDatabaseAdmin.getRequests().stream()
              .filter(request -> request instanceof UpdateDatabaseDdlRequest)
              .map(request -> (UpdateDatabaseDdlRequest) request)
              .toList();
      if (hbm2Ddl.equals("create")) {
        // 2 batches , 1 for dropping , 1 for creating
        assertEquals(2, requests.size());
        UpdateDatabaseDdlRequest dropRequest = requests.get(0);
        int index = -1;
        assertEquals(
            "drop sequence if exists customer_id_sequence", dropRequest.getStatements(++index));
        assertEquals(
            "drop sequence if exists invoice_id_sequence", dropRequest.getStatements(++index));
        assertEquals(
            "drop sequence if exists singer_id_sequence", dropRequest.getStatements(++index));
      } else {
        assertEquals(1, requests.size());
      }

      UpdateDatabaseDdlRequest request =
          hbm2Ddl.equals("create") ? requests.get(1) : requests.get(0);
      assertEquals(10, request.getStatementsCount());

      int index = -1;

      if (hbm2Ddl.equals("update")) {
        assertEquals(
            "create table Account (id int64 not null,amount numeric,name string(255)) PRIMARY KEY (id)",
            request.getStatements(++index));
        assertEquals(
            "create table Album (id int64 not null,singer int64) PRIMARY KEY (id)",
            request.getStatements(++index));
        assertEquals(
            "create table Customer (customerId int64 not null,name string(255)) PRIMARY KEY (customerId)",
            request.getStatements(++index));
        assertEquals(
            "create table Invoice (invoiceId int64 not null,number string(255) default ('9999'),customer_customerId int64) PRIMARY KEY (invoiceId)",
            request.getStatements(++index));
        assertEquals(
            "create table Singer (id int64 not null,name string(255)) PRIMARY KEY (id)",
            request.getStatements(++index));
        assertEquals(
            "create sequence if not exists customer_id_sequence options(sequence_kind=\"bit_reversed_positive\", start_with_counter=50000)",
            request.getStatements(++index));
        assertEquals(
            "create sequence if not exists invoice_id_sequence options(sequence_kind=\"bit_reversed_positive\")",
            request.getStatements(++index));
        assertEquals(
            "create sequence if not exists singer_id_sequence options(sequence_kind=\"bit_reversed_positive\")",
            request.getStatements(++index));
        assertEquals(
            "alter table Album add constraint fk_album_singer foreign key (singer) references Singer (id)",
            request.getStatements(++index));
        assertEquals(
            "alter table Invoice add constraint fk_invoice_customer foreign key (customer_customerId) references Customer (customerId) on delete cascade",
            request.getStatements(++index));
      } else {
        assertEquals(
            "create sequence if not exists customer_id_sequence options(sequence_kind=\"bit_reversed_positive\", start_with_counter=50000)",
            request.getStatements(++index));
        assertEquals(
            "create sequence if not exists invoice_id_sequence options(sequence_kind=\"bit_reversed_positive\")",
            request.getStatements(++index));
        assertEquals(
            "create sequence if not exists singer_id_sequence options(sequence_kind=\"bit_reversed_positive\")",
            request.getStatements(++index));
        assertEquals(
            "create table Account (amount numeric,id int64 not null,name string(255)) PRIMARY KEY (id)",
            request.getStatements(++index));
        assertEquals(
            "create table Album (id int64 not null,singer int64) PRIMARY KEY (id)",
            request.getStatements(++index));
        assertEquals(
            "create table Customer (customerId int64 not null,name string(255)) PRIMARY KEY (customerId)",
            request.getStatements(++index));
        assertEquals(
            "create table Invoice (customer_customerId int64,invoiceId int64 not null,number string(255) default ('9999')) PRIMARY KEY (invoiceId)",
            request.getStatements(++index));
        assertEquals(
            "create table Singer (id int64 not null,name string(255)) PRIMARY KEY (id)",
            request.getStatements(++index));
        assertEquals(
            "alter table Album add constraint fk_album_singer foreign key (singer) references Singer (id)",
            request.getStatements(++index));
        assertEquals(
            "alter table Invoice add constraint fk_invoice_customer foreign key (customer_customerId) references Customer (customerId) on delete cascade",
            request.getStatements(++index));
      }
    }
  }

  @Test
  public void testDropEmptySchema() {
    addDdlResponseToSpannerAdmin();

    //noinspection EmptyTryBlock
    try (SessionFactory ignore =
        createTestHibernateConfig(
                ImmutableList.of(
                    Singer.class, Album.class, Invoice.class, Customer.class, Account.class),
                ImmutableMap.of("hibernate.hbm2ddl.auto", "drop", "hibernate.show_sql", "true"))
            .buildSessionFactory()) {
      // do nothing, just generate the schema.
    }

    // Check the DDL statements that were generated.
    List<UpdateDatabaseDdlRequest> requests =
        mockDatabaseAdmin.getRequests().stream()
            .filter(request -> request instanceof UpdateDatabaseDdlRequest)
            .map(request -> (UpdateDatabaseDdlRequest) request)
            .toList();
    assertEquals(1, requests.size());
  }

  @Test
  public void testDropExistingSchema() {
    // Explicitly bind p2 to "" because SpannerDatabaseInfo now uses that for default namespace
    // lookups
    mockSpanner.putStatementResult(
        StatementResult.query(
            GET_TABLES_STATEMENT.toBuilder().bind("p2").to("").build(),
            ResultSet.newBuilder()
                .setMetadata(GET_TABLES_METADATA)
                .addRows(createTableRow("Account"))
                .addRows(createTableRow("Customer"))
                .addRows(createTableRow("customerId"))
                .addRows(createTableRow("Invoice"))
                .addRows(createTableRow("invoiceId"))
                .addRows(createTableRow("Singer"))
                .addRows(createTableRow("singerId"))
                .build()));
    mockSpanner.putStatementResult(
        StatementResult.query(
            GET_FOREIGN_KEYS_STATEMENT.toBuilder()
                .bind("p1")
                .to("%")
                .bind("p2")
                .to("%")
                .bind("p3")
                .to("INVOICE")
                .build(),
            ResultSet.newBuilder()
                .setMetadata(GET_FOREIGN_KEYS_METADATA)
                .addRows(
                    createForeignKeyRow(
                        "",
                        "Customer",
                        "customerId",
                        "",
                        "Invoice",
                        "customer_customerId",
                        1,
                        1,
                        "fk_invoice_customer"))
                .build()));

    addDdlResponseToSpannerAdmin();

    //noinspection EmptyTryBlock
    try (SessionFactory ignore =
        createTestHibernateConfig(
                ImmutableList.of(
                    Singer.class, Album.class, Invoice.class, Customer.class, Account.class),
                ImmutableMap.of("hibernate.hbm2ddl.auto", "drop", "hibernate.show_sql", "true"))
            .buildSessionFactory()) {
      // do nothing, just generate the schema.
    }

    // Check the DDL statements that were generated.
    List<UpdateDatabaseDdlRequest> requests =
        mockDatabaseAdmin.getRequests().stream()
            .filter(request -> request instanceof UpdateDatabaseDdlRequest)
            .map(request -> (UpdateDatabaseDdlRequest) request)
            .toList();
    assertEquals(1, requests.size());
    UpdateDatabaseDdlRequest request = requests.get(0);
    assertEquals(8, request.getStatementsCount());

    int index = -1;
    assertEquals(
        "alter table Invoice drop constraint fk_invoice_customer", request.getStatements(++index));
    assertEquals("drop table Account", request.getStatements(++index));
    assertEquals("drop table Customer", request.getStatements(++index));
    assertEquals("drop table Invoice", request.getStatements(++index));
    assertEquals("drop table Singer", request.getStatements(++index));
    assertEquals("drop sequence if exists customer_id_sequence", request.getStatements(++index));
    assertEquals("drop sequence if exists invoice_id_sequence", request.getStatements(++index));
    assertEquals("drop sequence if exists singer_id_sequence", request.getStatements(++index));
  }

  @Test
  public void testGenerateEmployeeSchema() {
    SpannerDialect.disableSpannerSequences();
    try {
      for (String hbm2Ddl : new String[] {"create-only", "update", "create"}) {
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
                .toList();
        assertEquals(1, requests.size());
        UpdateDatabaseDdlRequest request = requests.get(0);
        assertEquals(4, request.getStatementsCount());

        int index = -1;

        // Hibernate creates the columns in a different order when using 'update' instead of
        // 'create'.
        if (hbm2Ddl.equals("update")) {
          assertEquals(
              "create table Employee (id int64 not null,name string(255),manager_id int64) PRIMARY KEY (id)",
              request.getStatements(++index));
          assertEquals(
              "create table Employee_Sequence (next_val int64) PRIMARY KEY ()",
              request.getStatements(++index));
          assertEquals(
              "create index name_index on Employee (name)", request.getStatements(++index));
          assertEquals(
              "alter table Employee add constraint FKiralam2duuhr33k8a10aoc2t6 foreign key (manager_id) references Employee (id)",
              request.getStatements(++index));
        } else {
          assertEquals(
              "create table Employee (id int64 not null,manager_id int64,name string(255)) PRIMARY KEY (id)",
              request.getStatements(++index));
          assertEquals(
              "create table Employee_Sequence (next_val int64) PRIMARY KEY ()",
              request.getStatements(++index));
          assertEquals(
              "create index name_index on Employee (name)", request.getStatements(++index));
          assertEquals(
              "alter table Employee add constraint FKiralam2duuhr33k8a10aoc2t6 foreign key (manager_id) references Employee (id)",
              request.getStatements(++index));
        }
      }
    } finally {
      SpannerDialect.enableSpannerSequences();
    }
  }

  @Test
  public void testGenerateAirportSchema() {
    for (String hbm2Ddl : new String[] {"create-only", "update", "create"}) {
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
              .toList();
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
          "create unique index UKgc568wb30sampsuirwne5jqgh on Airplane (modelName)",
          request.getStatements(++index));
      assertEquals(
          "create unique index UKem0lqvwoqdwt29x0b0r010be on Airport_Airplane (airplanes_id)",
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
    SpannerDialect.disableSpannerSequences();

    try {
      for (String hbm2Ddl : new String[] {"create-only", "update", "create"}) {
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
                .toList();
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
            "create table Child (childId int64 not null,grandParentId int64 not null,"
                + "parentId int64 not null,name string(255)) "
                + "PRIMARY KEY (grandParentId,parentId,childId), INTERLEAVE IN PARENT Parent",
            request.getStatements(++index));
        assertEquals(
            "create table GrandParent_Sequence (next_val int64) PRIMARY KEY ()",
            request.getStatements(++index));
      }
    } finally {
      SpannerDialect.enableSpannerSequences();
    }
  }

  @Test
  public void testGenerateTestEntitySchema() {
    for (String hbm2Ddl : new String[] {"create-only", "update", "create"}) {
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
              .toList();
      assertEquals(1, requests.size());
      UpdateDatabaseDdlRequest request = requests.get(0);
      assertEquals(5, request.getStatementsCount());

      int index = -1;

      if (hbm2Ddl.equals("update")) {
        assertEquals(
            "create table `TestEntity_stringList` ("
                + "`TestEntity_ID1` int64 not null,"
                + "`TestEntity_id2` string(255) not null,"
                + "stringList string(255)) "
                + "PRIMARY KEY (`TestEntity_ID1`,`TestEntity_id2`,stringList)",
            request.getStatements(++index));
        assertEquals(
            "create table SubTestEntity (id string(255) not null,id1 int64,id2 string(255)) PRIMARY KEY (id)",
            request.getStatements(++index));
        assertEquals(
            "create table `test_table` ("
                + "`ID1` int64 not null,"
                + "id2 string(255) not null,"
                + "`boolColumn` bool,"
                + "floatVal float32 not null,"
                + "floatValStoredAsDouble float64 not null,"
                + "longVal int64 not null,"
                + "stringVal string(255)) PRIMARY KEY (`ID1`,id2)",
            request.getStatements(++index));
        assertEquals(
            "alter table `TestEntity_stringList` add constraint FK2is6fwy3079dmfhjot09x5och "
                + "foreign key (`TestEntity_ID1`, `TestEntity_id2`) references `test_table` (`ID1`, id2)",
            request.getStatements(++index));
      } else {
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
                + "floatVal float32 not null,"
                + "floatValStoredAsDouble float64 not null,"
                + "`ID1` int64 not null,"
                + "longVal int64 not null,"
                + "id2 string(255) not null,"
                + "stringVal string(255)) PRIMARY KEY (`ID1`,id2)",
            request.getStatements(++index));
        assertEquals(
            "alter table `TestEntity_stringList` add constraint FK2is6fwy3079dmfhjot09x5och "
                + "foreign key (`TestEntity_ID1`, `TestEntity_id2`) references `test_table` (`ID1`, id2)",
            request.getStatements(++index));
      }
    }
  }

  @Test
  public void testGenerateSequence() {
    addDdlResponseToSpannerAdmin();

    //noinspection EmptyTryBlock
    try (SessionFactory ignore =
        createTestHibernateConfig(
                ImmutableList.of(
                    SequenceEntity.class, AutoIdEntity.class, PooledSequenceEntity.class),
                ImmutableMap.of("hibernate.hbm2ddl.auto", "update", "hibernate.show_sql", "true"))
            .buildSessionFactory()) {
      // do nothing, just generate the schema.
    }

    // Check the DDL statements that were generated.
    List<UpdateDatabaseDdlRequest> requests =
        mockDatabaseAdmin.getRequests().stream()
            .filter(request -> request instanceof UpdateDatabaseDdlRequest)
            .map(request -> (UpdateDatabaseDdlRequest) request)
            .toList();
    assertEquals(1, requests.size());
    UpdateDatabaseDdlRequest request = requests.get(0);
    int expectedStatementCount = 6;
    assertEquals(expectedStatementCount, request.getStatementsCount());

    int index = -1;

    assertEquals(
        "create table AutoIdEntity (id int64 not null,name string(255)) PRIMARY KEY (id)",
        request.getStatements(++index));
    // This sequence is generated by the entity that has a no-arg @GeneratedValue annotation.
    // Hibernate 6 by default uses a pooled sequence with increment size 50 for this, and as Cloud
    // Spanner does not support pooled sequences, it falls back to a table.
    assertEquals(
        "create table AutoIdEntity_SEQ (next_val int64) PRIMARY KEY ()",
        request.getStatements(++index));
    assertEquals(
        "create table PooledSequenceEntity (id int64 not null,name string(255)) PRIMARY KEY (id)",
        request.getStatements(++index));
    assertEquals(
        "create table SequenceEntity (id int64 not null,name string(255)) PRIMARY KEY (id)",
        request.getStatements(++index));
    assertEquals(
        "create sequence if not exists pooled_sequence options(sequence_kind=\"bit_reversed_positive\")",
        request.getStatements(++index));
    assertEquals(
        "create sequence if not exists test_sequence options(sequence_kind=\"bit_reversed_positive\")",
        request.getStatements(++index));
    assertEquals(expectedStatementCount, ++index);
  }

  @Test
  public void testGenerateSequenceWithSequencesDisabled() {
    // Verify that we still get a table-backed sequence if we disable sequence support.
    SpannerDialect.disableSpannerSequences();

    try {
      for (String hbm2Ddl : new String[] {"create-only", "update", "create"}) {
        mockDatabaseAdmin.getRequests().clear();
        addDdlResponseToSpannerAdmin();

        //noinspection EmptyTryBlock
        try (SessionFactory ignore =
            createTestHibernateConfig(
                    ImmutableList.of(SequenceEntity.class, AutoIdEntity.class),
                    ImmutableMap.of("hibernate.hbm2ddl.auto", "update"))
                .buildSessionFactory()) {
          // do nothing, just generate the schema.
        }

        // Check the DDL statements that were generated.
        List<UpdateDatabaseDdlRequest> requests =
            mockDatabaseAdmin.getRequests().stream()
                .filter(request -> request instanceof UpdateDatabaseDdlRequest)
                .map(request -> (UpdateDatabaseDdlRequest) request)
                .toList();
        assertEquals(1, requests.size());
        UpdateDatabaseDdlRequest request = requests.get(0);
        assertEquals(4, request.getStatementsCount());

        int index = -1;

        if ("update".equals(hbm2Ddl)) {
          assertEquals(
              "create table AutoIdEntity (id int64 not null,name string(255)) PRIMARY KEY (id)",
              request.getStatements(++index));
          assertEquals(
              "create table AutoIdEntity_SEQ (next_val int64) PRIMARY KEY ()",
              request.getStatements(++index));
          assertEquals(
              "create table SequenceEntity (id int64 not null,name string(255)) PRIMARY KEY (id)",
              request.getStatements(++index));
          assertEquals(
              "create table test_sequence (next_val int64) PRIMARY KEY ()",
              request.getStatements(++index));
        } else {
          assertEquals(
              "create table AutoIdEntity (id int64 not null,name string(255)) PRIMARY KEY (id)",
              request.getStatements(++index));
          assertEquals(
              "create table AutoIdEntity_SEQ (next_val int64) PRIMARY KEY ()",
              request.getStatements(++index));
          assertEquals(
              "create table SequenceEntity (id int64 not null,name string(255)) PRIMARY KEY (id)",
              request.getStatements(++index));
          assertEquals(
              "create table test_sequence (next_val int64) PRIMARY KEY ()",
              request.getStatements(++index));
        }
      }
    } finally {
      SpannerDialect.enableSpannerSequences();
    }
  }

  @Test
  public void testGenerateIdentity() {
    addDdlResponseToSpannerAdmin();

    //noinspection EmptyTryBlock
    try (SessionFactory ignore =
        createTestHibernateConfig(
                ImmutableList.of(IdentityEntity.class),
                ImmutableMap.of("hibernate.hbm2ddl.auto", "update"))
            .buildSessionFactory()) {
      // do nothing, just generate the schema.
    }

    // Check the DDL statements that were generated.
    List<UpdateDatabaseDdlRequest> requests =
        mockDatabaseAdmin.getRequests().stream()
            .filter(request -> request instanceof UpdateDatabaseDdlRequest)
            .map(request -> (UpdateDatabaseDdlRequest) request)
            .toList();
    assertEquals(1, requests.size());
    UpdateDatabaseDdlRequest request = requests.get(0);
    assertEquals(1, request.getStatementsCount());
    String statement = request.getStatements(0);
    assertEquals(
        "create table IdentityEntity (id int64 not null GENERATED BY DEFAULT AS IDENTITY (BIT_REVERSED_POSITIVE),name string(255)) PRIMARY KEY (id)",
        statement);
  }

  @Test
  public void testBatchedSequenceEntity_CreateOnly() {
    addDdlResponseToSpannerAdmin();
    long sequenceBatchSize = 5L;
    String selectSequenceNextVals =
        "/* spanner.force_read_write_transaction=true */ "
            + "/* spanner.ignore_during_internal_retry=true */ "
            + " select get_next_sequence_value(sequence enhanced_sequence) AS n "
            + "from unnest(generate_array(1, 5))";
    String insertSql = "insert into PooledBitReversedSequenceEntity (name,id) values (@p1,@p2)";
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of(selectSequenceNextVals),
            ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(
                            StructType.newBuilder()
                                .addFields(
                                    Field.newBuilder()
                                        .setName("n")
                                        .setType(Type.newBuilder().setCode(TypeCode.INT64).build())
                                        .build())
                                .build())
                        .build())
                .addAllRows(
                    LongStream.rangeClosed(1L, sequenceBatchSize)
                        .mapToObj(
                            id ->
                                ListValue.newBuilder()
                                    .addValues(
                                        Value.newBuilder()
                                            .setStringValue(String.valueOf(Long.reverse(id)))
                                            .build())
                                    .build())
                        .collect(Collectors.toList()))
                .build()));
    mockSpanner.putStatementResult(
        StatementResult.update(
            Statement.newBuilder(insertSql)
                .bind("p1")
                .to("test1")
                .bind("p2")
                .to(Long.reverse(1L))
                .build(),
            1L));
    mockSpanner.putStatementResult(
        StatementResult.update(
            Statement.newBuilder(insertSql)
                .bind("p1")
                .to("test2")
                .bind("p2")
                .to(Long.reverse(2L))
                .build(),
            1L));

    try (SessionFactory sessionFactory =
            createTestHibernateConfig(
                    ImmutableList.of(
                        PooledBitReversedSequenceEntity.class, LegacySequenceEntity.class),
                    ImmutableMap.of(
                        Environment.HBM2DDL_AUTO,
                        "create-only",
                        Environment.STATEMENT_BATCH_SIZE,
                        "50"))
                .buildSessionFactory();
        Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      PooledBitReversedSequenceEntity entity = new PooledBitReversedSequenceEntity("test1");
      session.persist(entity);
      assertEquals(Long.reverse(1L), entity.getId());
      PooledBitReversedSequenceEntity entity2 = new PooledBitReversedSequenceEntity("test2");
      session.persist(entity2);
      assertEquals(Long.reverse(2L), entity2.getId());
      transaction.commit();
    }

    ExecuteSqlRequest sequenceRequest =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
            .filter(request -> request.getSql().equals(selectSequenceNextVals))
            .findFirst()
            .orElse(ExecuteSqlRequest.getDefaultInstance());
    assertTrue(sequenceRequest.hasTransaction());
    assertTrue(sequenceRequest.getTransaction().hasBegin());
    assertTrue(sequenceRequest.getTransaction().getBegin().hasReadWrite());
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class));
    ExecuteBatchDmlRequest insertRequest =
        mockSpanner.getRequestsOfType(ExecuteBatchDmlRequest.class).get(0);
    assertTrue(insertRequest.hasTransaction());
    assertTrue(insertRequest.getTransaction().hasBegin());
    assertTrue(insertRequest.getTransaction().getBegin().hasReadWrite());
    assertEquals(2, insertRequest.getStatementsCount());
    assertEquals(insertSql, insertRequest.getStatements(0).getSql());
    assertEquals(insertSql, insertRequest.getStatements(1).getSql());
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
    assertEquals(1, mockSpanner.countRequestsOfType(RollbackRequest.class));

    // Check the DDL statements that were generated.
    List<UpdateDatabaseDdlRequest> requests =
        mockDatabaseAdmin.getRequests().stream()
            .filter(request -> request instanceof UpdateDatabaseDdlRequest)
            .map(request -> (UpdateDatabaseDdlRequest) request)
            .toList();
    assertEquals(1, requests.size());
    UpdateDatabaseDdlRequest request = requests.get(0);
    assertEquals(4, request.getStatementsCount());

    int index = -1;

    assertEquals(
        "create sequence if not exists enhanced_sequence options(sequence_kind=\"bit_reversed_positive\", "
            + "start_with_counter=5000, skip_range_min=1, skip_range_max=1000)",
        request.getStatements(++index));
    assertEquals(
        "create sequence if not exists legacy_entity_sequence "
            + "options(sequence_kind=\"bit_reversed_positive\", start_with_counter=5000, "
            + "skip_range_min=1, skip_range_max=20000)",
        request.getStatements(++index));
    assertEquals(
        "create table LegacySequenceEntity (id int64 not null,name string(255)) PRIMARY KEY (id)",
        request.getStatements(++index));
    assertEquals(
        "create table PooledBitReversedSequenceEntity (id int64 not null,name string(255)) PRIMARY KEY (id)",
        request.getStatements(++index));
  }

  @Test
  public void testBatchedSequenceEntity_Update() {
    addDdlResponseToSpannerAdmin();

    // Setup schema results using explicit schema "" since the updated SpannerDatabaseInfo will use
    // that
    mockSpanner.putStatementResult(
        StatementResult.query(
            GET_TABLES_STATEMENT.toBuilder().bind("p2").to("").build(),
            ResultSet.newBuilder()
                .setMetadata(GET_TABLES_METADATA)
                .addRows(createTableRow("PooledBitReversedSequenceEntity"))
                .build()));
    mockSpanner.putStatementResult(
        StatementResult.query(
            GET_SEQUENCES_STATEMENT,
            ResultSet.newBuilder()
                .setMetadata(GET_SEQUENCES_METADATA)
                .addRows(createSequenceRow("enhanced_sequence"))
                .build()));
    mockSpanner.putStatementResult(
        StatementResult.query(
            GET_COLUMNS_STATEMENT.toBuilder().bind("p2").to("").build(),
            ResultSet.newBuilder()
                .setMetadata(GET_COLUMNS_METADATA)
                .addRows(
                    createColumnRow(
                        "PooledBitReversedSequenceEntity", "id", Types.BIGINT, "INT64", 1))
                .addRows(
                    createColumnRow(
                        "PooledBitReversedSequenceEntity",
                        "name",
                        Types.NVARCHAR,
                        "STRING(MAX)",
                        2))
                .build()));
    mockSpanner.putStatementResult(
        StatementResult.query(
            GET_INDEXES_STATEMENT.toBuilder().bind("p2").to("").build(),
            ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(StructType.newBuilder().build())
                        .build())
                .build()));

    long sequenceBatchSize = 5L;
    String selectSequenceNextVals =
        "/* spanner.force_read_write_transaction=true */ "
            + "/* spanner.ignore_during_internal_retry=true */ "
            + " select get_next_sequence_value(sequence enhanced_sequence) AS n "
            + "from unnest(generate_array(1, 5))";
    String insertSql = "insert into PooledBitReversedSequenceEntity (name,id) values (@p1,@p2)";
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of(selectSequenceNextVals),
            ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(
                            StructType.newBuilder()
                                .addFields(
                                    Field.newBuilder()
                                        .setName("n")
                                        .setType(Type.newBuilder().setCode(TypeCode.INT64).build())
                                        .build())
                                .build())
                        .build())
                .addAllRows(
                    LongStream.rangeClosed(1L, sequenceBatchSize)
                        .mapToObj(
                            id ->
                                ListValue.newBuilder()
                                    .addValues(
                                        Value.newBuilder()
                                            .setStringValue(String.valueOf(Long.reverse(id)))
                                            .build())
                                    .build())
                        .collect(Collectors.toList()))
                .build()));
    mockSpanner.putStatementResult(
        StatementResult.update(
            Statement.newBuilder(insertSql)
                .bind("p1")
                .to("test1")
                .bind("p2")
                .to(Long.reverse(1L))
                .build(),
            1L));
    mockSpanner.putStatementResult(
        StatementResult.update(
            Statement.newBuilder(insertSql)
                .bind("p1")
                .to("test2")
                .bind("p2")
                .to(Long.reverse(2L))
                .build(),
            1L));

    try (SessionFactory sessionFactory =
            createTestHibernateConfig(
                    ImmutableList.of(PooledBitReversedSequenceEntity.class),
                    ImmutableMap.of(
                        Environment.HBM2DDL_AUTO, "update", Environment.STATEMENT_BATCH_SIZE, "50"))
                .buildSessionFactory();
        Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      PooledBitReversedSequenceEntity entity = new PooledBitReversedSequenceEntity("test1");
      session.persist(entity);
      assertEquals(Long.reverse(1L), entity.getId());
      PooledBitReversedSequenceEntity entity2 = new PooledBitReversedSequenceEntity("test2");
      session.persist(entity2);
      assertEquals(Long.reverse(2L), entity2.getId());

      transaction.commit();
    }

    ExecuteSqlRequest sequenceRequest =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
            .filter(request -> request.getSql().equals(selectSequenceNextVals))
            .findFirst()
            .orElse(ExecuteSqlRequest.getDefaultInstance());
    assertTrue(sequenceRequest.hasTransaction());
    assertTrue(sequenceRequest.getTransaction().hasBegin());
    assertTrue(sequenceRequest.getTransaction().getBegin().hasReadWrite());
    // Note the existence of an ExecuteBatchDml request. This verifies that our bit-reversed
    // sequence generator supports batch inserts.
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class));
    ExecuteBatchDmlRequest insertRequest =
        mockSpanner.getRequestsOfType(ExecuteBatchDmlRequest.class).get(0);
    assertTrue(insertRequest.hasTransaction());
    assertTrue(insertRequest.getTransaction().hasBegin());
    assertTrue(insertRequest.getTransaction().getBegin().hasReadWrite());
    assertEquals(2, insertRequest.getStatementsCount());
    assertEquals(insertSql, insertRequest.getStatements(0).getSql());
    assertEquals(insertSql, insertRequest.getStatements(1).getSql());
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
    assertEquals(1, mockSpanner.countRequestsOfType(RollbackRequest.class));

    // Check that there were no DDL statements generated as the data model is up-to-date.
    List<UpdateDatabaseDdlRequest> requests =
        mockDatabaseAdmin.getRequests().stream()
            .filter(request -> request instanceof UpdateDatabaseDdlRequest)
            .map(request -> (UpdateDatabaseDdlRequest) request)
            .toList();
    assertEquals(0, requests.size());
  }
}
