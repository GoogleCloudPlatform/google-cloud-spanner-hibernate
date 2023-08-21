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

import com.google.cloud.spanner.hibernate.entities.Customer;
import com.google.cloud.spanner.hibernate.entities.Employee;
import com.google.cloud.spanner.hibernate.entities.Invoice;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mockrunner.mock.jdbc.JDBCMockObjectFactory;
import com.mockrunner.mock.jdbc.MockConnection;
import com.mockrunner.mock.jdbc.MockDriver;
import com.mockrunner.mock.jdbc.MockResultSet;
import com.mockrunner.mock.jdbc.MockResultSetMetaData;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.Before;
import org.junit.Test;

/** Verifies correct DDL generation of update table statements. */
public class GeneratedUpdateTableStatementsTests {

  private StandardServiceRegistry registry;

  private JDBCMockObjectFactory jdbcMockObjectFactory;

  private MockConnection defaultConnection;
  private MockConnection ddlBatchMockConnection;
  private MockConnection extractorMockConnection;

  /** Set up the metadata for Hibernate to generate schema statements. */
  @Before
  public void setup() {
    this.jdbcMockObjectFactory =
        new JDBCMockObjectFactory() {
          @Override
          public MockDriver createMockDriver() {
            return new MockDriver() {
              final List<Connection> connections = new ArrayList<>();
              int index = 0;

              @Override
              public void setupConnection(Connection connection) {
                connections.add(connection);
              }

              @Override
              public Connection connect(String url, Properties info) {
                Connection connection = connections.get(Math.min(index, connections.size() - 1));
                index++;
                return connection;
              }
            };
          }
        };
    this.jdbcMockObjectFactory.registerMockDriver();

    defaultConnection = this.jdbcMockObjectFactory.getMockConnection();
    ddlBatchMockConnection = this.jdbcMockObjectFactory.createMockConnection();
    extractorMockConnection = this.jdbcMockObjectFactory.createMockConnection();

    this.jdbcMockObjectFactory.getMockDriver().setupConnection(ddlBatchMockConnection);
    this.jdbcMockObjectFactory.getMockDriver().setupConnection(extractorMockConnection);

    this.registry =
        new StandardServiceRegistryBuilder()
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
    SpannerDialect.disableSpannerSequences();
    setupTestTables("Hello");
    
    try {
      List<String> sqlStrings =
          ddlBatchMockConnection.getStatementResultSetHandler().getExecutedStatements();
      assertThat(sqlStrings)
          .containsExactly(
              "START BATCH DDL",
              "create table Employee (id INT64 not null,name STRING(255),manager_id INT64) "
                  + "PRIMARY KEY (id)",
              "create table hibernate_sequence (next_val INT64) PRIMARY KEY ()",
              "create index name_index on Employee (name)",
              "alter table Employee add constraint FKiralam2duuhr33k8a10aoc2t6 "
                  + "foreign key (manager_id) references Employee (id)",
              "RUN BATCH",
              "INSERT INTO hibernate_sequence (next_val) VALUES(1)");
    } finally {
      SpannerDialect.enableSpannerSequences();
    }
  }

  @Test
  public void testUpdateStatements_createTables_withSequencesEnabled() throws SQLException {
    setupTestTables("Hello");

    List<String> sqlStrings =
        ddlBatchMockConnection.getStatementResultSetHandler().getExecutedStatements();
    assertThat(sqlStrings)
        .containsExactly(
            "START BATCH DDL",
            "create table Employee (id INT64 not null,name STRING(255),manager_id INT64) "
                + "PRIMARY KEY (id)",
            "create table hibernate_sequence (next_val INT64) PRIMARY KEY ()",
            "create index name_index on Employee (name)",
            "alter table Employee add constraint FKiralam2duuhr33k8a10aoc2t6 "
                + "foreign key (manager_id) references Employee (id)",
            "RUN BATCH",
            "INSERT INTO hibernate_sequence (next_val) VALUES(1)");
  }

  @Test
  public void testUpdateStatements_alterTables() throws SQLException {
    setupTestTables(ImmutableMap.of("Employee", ImmutableList.of("id", "name", "manager_id")));
    List<String> sqlStrings =
        ddlBatchMockConnection.getStatementResultSetHandler().getExecutedStatements();
    // The "alter table Employee ADD COLUMN" statements are no longer included in the generated
    // schema when updating an existing table. This schema change was introduced from Hibernate
    // 5.5.2 onwards.
    assertThat(sqlStrings)
        .containsExactly(
            "START BATCH DDL",
            "create table hibernate_sequence (next_val INT64) PRIMARY KEY ()",
            "create index name_index on Employee (name)",
            "alter table Employee add constraint FKiralam2duuhr33k8a10aoc2t6 "
                + "foreign key (manager_id) references Employee (id)",
            "RUN BATCH",
            "INSERT INTO hibernate_sequence (next_val) VALUES(1)");
  }

  @Test
  public void testUpdateStatements_withExistingTables_withForeignKeys() throws SQLException {
    setupTestTablesWithForeignKeys(
        ImmutableMap.of(
            "Customer", ImmutableList.of("customerId", "name"),
            "customerId", ImmutableList.of("next_val"),
            "Invoice", ImmutableList.of("invoiceId", "number", "customer_customerId"),
            "invoiceId", ImmutableList.of("next_val")));

    List<String> sqlStrings =
        ddlBatchMockConnection.getStatementResultSetHandler().getExecutedStatements();
    assertThat(sqlStrings).containsExactly("START BATCH DDL", "RUN BATCH");
    assertEquals(
        ImmutableList.of(),
        ddlBatchMockConnection.getPreparedStatementResultSetHandler().getExecutedStatements());

    assertEquals(
        ImmutableList.of(),
        extractorMockConnection.getStatementResultSetHandler().getExecutedStatements());
    assertEquals(
        ImmutableList.of("select * from `Customer` where 1=0"),
        extractorMockConnection.getPreparedStatementResultSetHandler().getExecutedStatements());
  }

  /** Sets up which pre-existing tables that Hibernate sees. */
  private void setupTestTables(String... tables) throws SQLException {
    defaultConnection.setMetaData(MockJdbcUtils.metaDataBuilder().setTables(tables).build());
    extractorMockConnection.setMetaData(MockJdbcUtils.metaDataBuilder().setTables(tables).build());

    Metadata metadata =
        new MetadataSources(this.registry).addAnnotatedClass(Employee.class).buildMetadata();

    Session session = metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    session.close();
  }

  private void setupTestTables(Map<String, List<String>> tablesAndColumns) throws SQLException {
    defaultConnection.setMetaData(
        MockJdbcUtils.metaDataBuilder().setTables(tablesAndColumns).build());
    extractorMockConnection.setMetaData(
        MockJdbcUtils.metaDataBuilder().setTables(tablesAndColumns).build());

    Metadata metadata =
        new MetadataSources(this.registry).addAnnotatedClass(Employee.class).buildMetadata();

    Session session = metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    session.close();
  }

  private void setupTestTablesWithForeignKeys(Map<String, List<String>> tables)
      throws SQLException {
    defaultConnection.setMetaData(
        MockJdbcUtils.metaDataBuilder()
            .setTables(tables)
            .setImportedKeys(
                "Customer", "customerId", "Invoice", "customer_customerId", "fk_invoice_customer")
            .build());
    extractorMockConnection.setMetaData(
        MockJdbcUtils.metaDataBuilder()
            .setTables(tables)
            .setImportedKeys(
                "Customer", "customerId", "Invoice", "customer_customerId", "fk_invoice_customer")
            .build());
    MockResultSetMetaData mockResultSetMetaData = new MockResultSetMetaData();
    mockResultSetMetaData.setColumnCount(2);
    mockResultSetMetaData.setColumnName(1, "customerId");
    mockResultSetMetaData.setColumnTypeName(1, "INT64");
    mockResultSetMetaData.setColumnName(2, "name");
    mockResultSetMetaData.setColumnTypeName(2, "STRING");
    MockResultSet resultSet = new MockResultSet("invoice_columns");
    resultSet.setResultSetMetaData(mockResultSetMetaData);
    extractorMockConnection
        .getPreparedStatementResultSetHandler()
        .prepareResultSet("select * from `Customer` where 1=0", resultSet);

    Metadata metadata =
        new MetadataSources(this.registry)
            .addAnnotatedClass(Customer.class)
            .addAnnotatedClass(Invoice.class)
            .buildMetadata();

    Session session = metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    session.close();
  }
}
