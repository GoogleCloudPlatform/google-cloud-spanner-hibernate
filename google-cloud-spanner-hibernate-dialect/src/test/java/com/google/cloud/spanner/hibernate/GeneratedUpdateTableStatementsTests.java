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

import com.google.cloud.spanner.hibernate.entities.Employee;
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

public class GeneratedUpdateTableStatementsTests {

  private StandardServiceRegistry registry;

  private JDBCMockObjectFactory jdbcMockObjectFactory;

  private MockConnection mockConnection;

  /**
   * Set up the metadata for Hibernate to generate schema statements.
   */
  @Before
  public void setup() {
    this.jdbcMockObjectFactory = new JDBCMockObjectFactory();
    this.jdbcMockObjectFactory.registerMockDriver();

    mockConnection = this.jdbcMockObjectFactory.createMockConnection();

    this.jdbcMockObjectFactory
        .getMockDriver()
        .setupConnection(mockConnection);

    this.registry = new StandardServiceRegistryBuilder()
        .applySetting("hibernate.dialect", SpannerDialect.class.getName())
        // must NOT set a driver class name so that Hibernate will use java.sql.DriverManager
        // and discover the only mock driver we have set up.
        .applySetting("hibernate.connection.url", "unused")
        .applySetting("hibernate.connection.username", "unused")
        .applySetting("hibernate.connection.password", "unused")
        .applySetting("hibernate.hbm2ddl.auto", "update")
        .build();
  }

  @Test
  public void testUpdateStatements_createTables() throws SQLException {
    setupTestTables("Hello");

    List<String> sqlStrings = mockConnection.getStatementResultSetHandler().getExecutedStatements();
    assertThat(sqlStrings).containsExactly(
        "START BATCH DDL",
        "create table Employee (id INT64 not null,name STRING(255),manager_id INT64) "
            + "PRIMARY KEY (id)",
        "create table hibernate_sequence (next_val INT64) PRIMARY KEY ()",
        "create index name_index on Employee (name)",
        "alter table Employee add constraint FKiralam2duuhr33k8a10aoc2t6 "
            + "foreign key (manager_id) references Employee (id)",
        "RUN BATCH",
        "INSERT INTO hibernate_sequence (next_val) VALUES(1)"
    );
  }

  @Test
  public void testUpdateStatements_alterTables() throws SQLException {
    setupTestTables("Employee");

    List<String> sqlStrings = mockConnection.getStatementResultSetHandler().getExecutedStatements();
    assertThat(sqlStrings).containsExactly(
        "START BATCH DDL",
        "alter table Employee ADD COLUMN id INT64 not null",
        "alter table Employee ADD COLUMN name STRING(255)",
        "alter table Employee ADD COLUMN manager_id INT64",
        "create table hibernate_sequence (next_val INT64) PRIMARY KEY ()",
        "create index name_index on Employee (name)",
        "alter table Employee add constraint FKiralam2duuhr33k8a10aoc2t6 "
            + "foreign key (manager_id) references Employee (id)",
        "RUN BATCH",
        "INSERT INTO hibernate_sequence (next_val) VALUES(1)"
    );
  }

  /**
   * Sets up which pre-existing tables that Hibernate sees.
   */
  private void setupTestTables(String... tables) throws SQLException {
    mockConnection.setMetaData(MockJdbcUtils.metaDataBuilder().setTables(tables).build());

    Metadata metadata =
        new MetadataSources(this.registry)
            .addAnnotatedClass(Employee.class)
            .buildMetadata();

    Session session = metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    session.close();
  }
}
