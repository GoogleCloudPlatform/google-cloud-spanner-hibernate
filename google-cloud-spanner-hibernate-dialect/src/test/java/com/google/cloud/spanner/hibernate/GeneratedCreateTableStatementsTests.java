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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.hibernate.entities.Account;
import com.google.cloud.spanner.hibernate.entities.Airplane;
import com.google.cloud.spanner.hibernate.entities.Airport;
import com.google.cloud.spanner.hibernate.entities.Child;
import com.google.cloud.spanner.hibernate.entities.Customer;
import com.google.cloud.spanner.hibernate.entities.Employee;
import com.google.cloud.spanner.hibernate.entities.GrandParent;
import com.google.cloud.spanner.hibernate.entities.Parent;
import com.mockrunner.mock.jdbc.JDBCMockObjectFactory;
import com.mockrunner.mock.jdbc.MockConnection;
import java.sql.SQLException;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Namespace.Name;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Table;
import org.junit.Before;
import org.junit.Test;

/** Verifies correct DDL generation of create table statements. */
public class GeneratedCreateTableStatementsTests {

  private StandardServiceRegistry registry;

  private MockConnection connection;

  /** Set up the metadata for Hibernate to generate schema statements. */
  @Before
  public void setup() throws SQLException {
    JDBCMockObjectFactory jdbcMockObjectFactory = new JDBCMockObjectFactory();
    jdbcMockObjectFactory.registerMockDriver();

    this.connection = jdbcMockObjectFactory.getMockConnection();
    this.connection.setMetaData(MockJdbcUtils.metaDataBuilder().build());
    jdbcMockObjectFactory.getMockDriver().setupConnection(this.connection);

    this.registry =
        new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", SpannerDialect.class.getName())
            // must NOT set a driver class name so that Hibernate will use java.sql.DriverManager
            // and discover the only mock driver we have set up.
            .applySetting("hibernate.connection.url", "unused")
            .applySetting("hibernate.connection.username", "unused")
            .applySetting("hibernate.connection.password", "unused")
            .applySetting("hibernate.hbm2ddl.auto", "create-drop")
            .build();
  }

  @Test
  public void testCreateInterleavedTables() {
    SpannerDialect.disableSpannerSequences();
    try {
      Metadata metadata =
          new MetadataSources(this.registry)
              .addAnnotatedClass(Child.class)
              .addAnnotatedClass(GrandParent.class)
              .addAnnotatedClass(Parent.class)
              .buildMetadata();

      Session session = metadata.buildSessionFactory().openSession();
      session.beginTransaction();
      session.close();

      List<String> sqlStrings = connection.getStatementResultSetHandler().getExecutedStatements();
      assertThat(sqlStrings)
          .containsExactly(
              "START BATCH DDL",
              "RUN BATCH",
              "START BATCH DDL",
              "create table GrandParent (grandParentId int64 not null,name string(255)) "
                  + "PRIMARY KEY (grandParentId)",
              "create table Parent ("
                  + "grandParentId int64 not null,parentId int64 not null,name string(255)) "
                  + "PRIMARY KEY (grandParentId,parentId), "
                  + "INTERLEAVE IN PARENT GrandParent",
              "create table Child (childId int64 not null,grandParentId int64 not null,"
                  + "parentId int64 not null,name string(255)) "
                  + "PRIMARY KEY (grandParentId,parentId,childId), "
                  + "INTERLEAVE IN PARENT Parent",
              "create table GrandParent_Sequence (next_val int64) PRIMARY KEY ()",
              "RUN BATCH",
              "insert into GrandParent_Sequence values ( 1 )");
    } finally {
      SpannerDialect.enableSpannerSequences();
    }
  }

  @Test
  public void testCreateInterleavedTablesWithSequencesEnabled() {
    Metadata metadata =
        new MetadataSources(this.registry)
            .addAnnotatedClass(Child.class)
            .addAnnotatedClass(GrandParent.class)
            .addAnnotatedClass(Parent.class)
            .buildMetadata();

    Session session = metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    session.close();

    List<String> sqlStrings = connection.getStatementResultSetHandler().getExecutedStatements();

    assertThat(sqlStrings)
        .containsExactly(
            "START BATCH DDL",
            "drop sequence GrandParent_Sequence",
            "RUN BATCH",
            "START BATCH DDL",
            "create sequence GrandParent_Sequence options(sequence_kind=\"bit_reversed_positive\")",
            "create table GrandParent (grandParentId int64 not null,name string(255)) "
                + "PRIMARY KEY (grandParentId)",
            "create table Parent (grandParentId int64 not null,parentId int64 not null,"
                + "name string(255)) PRIMARY KEY (grandParentId,parentId), "
                + "INTERLEAVE IN PARENT GrandParent",
            "create table Child (childId int64 not null,grandParentId int64 not null,"
                + "parentId int64 not null,name string(255)) "
                + "PRIMARY KEY (grandParentId,parentId,childId), "
                + "INTERLEAVE IN PARENT Parent",
            "RUN BATCH");
  }

  @Test
  public void testCreateTables() {
    SpannerDialect.disableSpannerSequences();
    try {
      Metadata metadata =
          new MetadataSources(this.registry).addAnnotatedClass(Employee.class).buildMetadata();

      Session session = metadata.buildSessionFactory().openSession();
      session.beginTransaction();
      session.close();

      List<String> sqlStrings =
          this.connection.getStatementResultSetHandler().getExecutedStatements();

      assertThat(sqlStrings)
          .containsExactly(
              "START BATCH DDL",
              "RUN BATCH",
              "START BATCH DDL",
              "create table Employee (id int64 not null,manager_id int64,name string(255)) "
                  + "PRIMARY KEY (id)",
              "create table Employee_Sequence (next_val int64) PRIMARY KEY ()",
              "create index name_index on Employee (name)",
              "alter table Employee add constraint FKiralam2duuhr33k8a10aoc2t6 "
                  + "foreign key (manager_id) references Employee (id)",
              "RUN BATCH",
              "insert into Employee_Sequence values ( 1 )");
    } finally {
      SpannerDialect.enableSpannerSequences();
    }
  }

  @Test
  public void testCreateTablesWithSequencesEnabled() {
    Metadata metadata =
        new MetadataSources(this.registry).addAnnotatedClass(Employee.class).buildMetadata();

    Session session = metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    session.close();

    List<String> sqlStrings =
        this.connection.getStatementResultSetHandler().getExecutedStatements();

    assertThat(sqlStrings)
        .containsExactly(
            "START BATCH DDL",
            "drop sequence Employee_Sequence",
            "RUN BATCH",
            "START BATCH DDL",
            "create sequence Employee_Sequence options(sequence_kind=\"bit_reversed_positive\")",
            "create table Employee (id int64 not null,manager_id int64,name string(255)) "
                + "PRIMARY KEY (id)",
            "create index name_index on Employee (name)",
            "alter table Employee add constraint FKiralam2duuhr33k8a10aoc2t6 "
                + "foreign key (manager_id) references Employee (id)",
            "RUN BATCH");
  }

  @Test
  public void testCreateNumericColumn() {
    Metadata metadata =
        new MetadataSources(this.registry).addAnnotatedClass(Account.class).buildMetadata();

    Session session = metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    session.close();

    List<String> sqlStrings =
        this.connection.getStatementResultSetHandler().getExecutedStatements();

    assertThat(sqlStrings)
        .containsExactly(
            "START BATCH DDL",
            "RUN BATCH",
            "START BATCH DDL",
            "create table Account (amount numeric,id int64 not null,name string(255)) PRIMARY KEY (id)",
            "RUN BATCH");
  }

  @Test
  public void testCreateBitReversedSequenceTable() {
    Metadata metadata =
        new MetadataSources(this.registry).addAnnotatedClass(Customer.class).buildMetadata();

    Session session = metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    session.close();

    List<String> sqlStrings =
        this.connection.getStatementResultSetHandler().getExecutedStatements();

    assertThat(sqlStrings)
        .containsExactly(
            "START BATCH DDL",
            "RUN BATCH",
            "START BATCH DDL",
            "create table Customer (customerId int64 not null,name string(255)) "
                + "PRIMARY KEY (customerId)",
            "create table customerId (next_val int64) PRIMARY KEY ()",
            "RUN BATCH",
            "insert into customerId (next_val) values ( 50000 )");
  }

  @Test
  public void testDropTables() throws SQLException {
    SpannerDialect.disableSpannerSequences();
    try {
      Metadata metadata =
          new MetadataSources(this.registry).addAnnotatedClass(Employee.class).buildMetadata();
      Namespace namespace = mock(Namespace.class);
      when(namespace.getPhysicalName())
          .thenReturn(new Name(Identifier.toIdentifier(""), Identifier.toIdentifier("")));
      Table table = new Table("orm", namespace, Identifier.toIdentifier("Employee"), false);

      this.connection.setMetaData(
          MockJdbcUtils.metaDataBuilder()
              .setTables("Employee", "Employee_Sequence")
              .setIndices(table, "name_index")
              .build());

      Session session = metadata.buildSessionFactory().openSession();
      session.beginTransaction();
      session.close();

      List<String> sqlStrings =
          this.connection.getStatementResultSetHandler().getExecutedStatements();

      assertThat(sqlStrings)
          .startsWith(
              "START BATCH DDL",
              "drop index if exists name_index",
              "drop table `Employee`",
              "drop table `Employee_Sequence`",
              "RUN BATCH");
    } finally {
      SpannerDialect.enableSpannerSequences();
    }
  }

  @Test
  public void testDropTablesWithSequencesEnabled() throws SQLException {
    Metadata metadata =
        new MetadataSources(this.registry).addAnnotatedClass(Employee.class).buildMetadata();
    Namespace namespace = mock(Namespace.class);
    when(namespace.getPhysicalName())
        .thenReturn(new Name(Identifier.toIdentifier(""), Identifier.toIdentifier("")));
    Table table = new Table("orm", namespace, Identifier.toIdentifier("Employee"), false);

    this.connection.setMetaData(
        MockJdbcUtils.metaDataBuilder()
            .setTables("Employee")
            .setIndices(table, "name_index")
            .build());

    Session session = metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    session.close();

    List<String> sqlStrings =
        this.connection.getStatementResultSetHandler().getExecutedStatements();

    assertThat(sqlStrings)
        .startsWith(
            "START BATCH DDL",
            "drop index if exists name_index",
            "drop table `Employee`",
            "drop sequence Employee_Sequence",
            "RUN BATCH");
  }

  @Test
  public void testCreateUniqueIndexes_uniqueColumn() throws SQLException {
    Metadata metadata =
        new MetadataSources(this.registry).addAnnotatedClass(Airplane.class).buildMetadata();

    Session session = metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    session.close();

    List<String> sqlStrings =
        this.connection.getStatementResultSetHandler().getExecutedStatements();

    assertThat(sqlStrings)
        .containsExactly(
            "START BATCH DDL",
            "RUN BATCH",
            "START BATCH DDL",
            "create table Airplane (id string(36) not null,modelName string(255)) PRIMARY KEY (id)",
            "create unique index UK_gc568wb30sampsuirwne5jqgh on Airplane (modelName)",
            "RUN BATCH");
  }

  @Test
  public void testCreateUniqueIndexes_oneToMany() throws SQLException {
    Metadata metadata =
        new MetadataSources(this.registry)
            .addAnnotatedClass(Airport.class)
            .addAnnotatedClass(Airplane.class)
            .buildMetadata();

    Session session = metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    session.close();

    List<String> sqlStrings =
        this.connection.getStatementResultSetHandler().getExecutedStatements();

    // Note that Hibernate generates a unique column for @OneToMany relationships because
    // one object is mapped to many others; this is distinct from the @ManyToMany case.
    // See: https://hibernate.atlassian.net/browse/HHH-3410
    assertThat(sqlStrings)
        .containsExactly(
            "START BATCH DDL",
            "RUN BATCH",
            "START BATCH DDL",
            "create table Airplane (id string(36) not null,modelName string(255)) PRIMARY KEY (id)",
            "create table Airport (id string(36) not null) PRIMARY KEY (id)",
            "create table Airport_Airplane (Airport_id string(36) not null,"
                + "airplanes_id string(36) not null) PRIMARY KEY (Airport_id,airplanes_id)",
            "create unique index UK_gc568wb30sampsuirwne5jqgh on Airplane (modelName)",
            "create unique index UK_em0lqvwoqdwt29x0b0r010be on Airport_Airplane (airplanes_id)",
            "alter table Airport_Airplane add constraint FKkn0enwaxbwk7csf52x0eps73d "
                + "foreign key (airplanes_id) references Airplane (id)",
            "alter table Airport_Airplane add constraint FKh186t28ublke8o13fo4ppogs7 "
                + "foreign key (Airport_id) references Airport (id)",
            "RUN BATCH");
  }

  @Test
  public void testStartBatchDdlFails() {
    testBatchFailure("START BATCH DDL");
  }

  @Test
  public void testRunBatchFails() {
    testBatchFailure("RUN BATCH");
  }

  private void testBatchFailure(String batchCommand) {
    this.connection
        .getStatementResultSetHandler()
        .prepareThrowsSQLException(batchCommand, new SQLException("test exception"));
    Metadata metadata =
        new MetadataSources(this.registry).addAnnotatedClass(Account.class).buildMetadata();

    // Without the custom DdlTransactionIsolater that is returned by SpannerSchemaManagementTool,
    // the SQLException would just be silently ignored by Hibernate.
    SpannerException spannerException =
        assertThrows(SpannerException.class, metadata::buildSessionFactory);
    assertEquals("UNKNOWN: test exception", spannerException.getMessage());
  }
}
