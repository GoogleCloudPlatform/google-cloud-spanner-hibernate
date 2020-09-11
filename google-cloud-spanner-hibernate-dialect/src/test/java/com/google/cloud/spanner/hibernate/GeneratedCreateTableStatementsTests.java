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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.cloud.spanner.hibernate.entities.Airplane;
import com.google.cloud.spanner.hibernate.entities.Airport;
import com.google.cloud.spanner.hibernate.entities.Child;
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
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.Before;
import org.junit.Test;

public class GeneratedCreateTableStatementsTests {

  private StandardServiceRegistry registry;

  private MockConnection connection;

  /**
   * Set up the metadata for Hibernate to generate schema statements.
   */
  @Before
  public void setup() throws SQLException {
    JDBCMockObjectFactory jdbcMockObjectFactory = new JDBCMockObjectFactory();
    jdbcMockObjectFactory.registerMockDriver();

    this.connection = jdbcMockObjectFactory.getMockConnection();
    this.connection.setMetaData(MockJdbcUtils.metaDataBuilder().build());
    jdbcMockObjectFactory.getMockDriver().setupConnection(this.connection);

    this.registry = new StandardServiceRegistryBuilder()
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
    Metadata metadata =
        new MetadataSources(this.registry)
            .addAnnotatedClass(Child.class)
            .addAnnotatedClass(GrandParent.class)
            .addAnnotatedClass(Parent.class)
            .buildMetadata();

    Session session = metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    session.close();

    List<String> sqlStrings =
        connection.getStatementResultSetHandler().getExecutedStatements();

    assertThat(sqlStrings).containsExactly(
        "START BATCH DDL",
        "RUN BATCH",
        "START BATCH DDL",
        "create table GrandParent (grandParentId INT64 not null,name STRING(255)) "
            + "PRIMARY KEY (grandParentId)",
        "create table Parent (grandParentId INT64 not null,"
            + "parentId INT64 not null,name STRING(255)) PRIMARY KEY (grandParentId,parentId), "
            + "INTERLEAVE IN PARENT GrandParent",
        "create table Child (childId INT64 not null,grandParentId INT64 not null,"
            + "parentId INT64 not null,name STRING(255)) "
            + "PRIMARY KEY (grandParentId,parentId,childId), "
            + "INTERLEAVE IN PARENT Parent",
        "create table hibernate_sequence (next_val INT64) PRIMARY KEY ()",
        "RUN BATCH",
        "INSERT INTO hibernate_sequence (next_val) VALUES(1)"
    );
  }

  @Test
  public void testCreateTables() {
    Metadata metadata =
        new MetadataSources(this.registry)
            .addAnnotatedClass(Employee.class)
            .buildMetadata();

    Session session = metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    session.close();

    List<String> sqlStrings =
        this.connection.getStatementResultSetHandler().getExecutedStatements();

    assertThat(sqlStrings).containsExactly(
        "START BATCH DDL",
        "RUN BATCH",
        "START BATCH DDL",
        "create table Employee "
            + "(id INT64 not null,name STRING(255),manager_id INT64) PRIMARY KEY (id)",
        "create table hibernate_sequence (next_val INT64) PRIMARY KEY ()",
        "create index name_index on Employee (name)",
        "alter table Employee add constraint FKiralam2duuhr33k8a10aoc2t6 "
            + "foreign key (manager_id) references Employee (id)",
        "RUN BATCH",
        "INSERT INTO hibernate_sequence (next_val) VALUES(1)"
    );
  }

  @Test
  public void testDropTables() throws SQLException {
    Metadata metadata =
        new MetadataSources(this.registry)
            .addAnnotatedClass(Employee.class)
            .buildMetadata();

    this.connection.setMetaData(MockJdbcUtils.metaDataBuilder()
        .setTables("Employee", "hibernate_sequence")
        .setIndices("name_index")
        .build());

    Session session = metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    session.close();

    List<String> sqlStrings =
        this.connection.getStatementResultSetHandler().getExecutedStatements();

    assertThat(sqlStrings).startsWith(
        "START BATCH DDL",
        "drop index name_index",
        "drop table Employee",
        "drop table hibernate_sequence",
        "RUN BATCH"
    );
  }

  @Test
  public void testCreateUniqueIndexes_uniqueColumn() throws SQLException {
    Metadata metadata =
        new MetadataSources(this.registry)
            .addAnnotatedClass(Airplane.class)
            .buildMetadata();

    Session session = metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    session.close();

    List<String> sqlStrings =
        this.connection.getStatementResultSetHandler().getExecutedStatements();

    assertThat(sqlStrings).containsExactly(
        "START BATCH DDL",
        "RUN BATCH",
        "START BATCH DDL",
        "create table Airplane (id STRING(255) not null,modelName STRING(255)) PRIMARY KEY (id)",
        "create unique index UK_gc568wb30sampsuirwne5jqgh on Airplane (modelName)",
        "RUN BATCH"
    );
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
    assertThat(sqlStrings).containsExactly(
        "START BATCH DDL",
        "RUN BATCH",
        "START BATCH DDL",
        "create table Airplane (id STRING(255) not null,modelName STRING(255)) PRIMARY KEY (id)",
        "create table Airport (id STRING(255) not null) PRIMARY KEY (id)",
        "create table Airport_Airplane (Airport_id STRING(255) not null,"
            + "airplanes_id STRING(255) not null) PRIMARY KEY (Airport_id,airplanes_id)",
        "create unique index UK_gc568wb30sampsuirwne5jqgh on Airplane (modelName)",
        "create unique index UK_em0lqvwoqdwt29x0b0r010be on Airport_Airplane (airplanes_id)",
        "alter table Airport_Airplane add constraint FKkn0enwaxbwk7csf52x0eps73d "
            + "foreign key (airplanes_id) references Airplane (id)",
        "alter table Airport_Airplane add constraint FKh186t28ublke8o13fo4ppogs7 "
            + "foreign key (Airport_id) references Airport (id)",
        "RUN BATCH"
    );
  }
}
