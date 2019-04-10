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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.cloud.spanner.hibernate.entities.Employee;
import com.google.cloud.spanner.hibernate.entities.TestEntity;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.Entity;

import com.mockrunner.mock.jdbc.JDBCMockObjectFactory;
import org.hibernate.AnnotationException;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for SpannerTableExporter.
 *
 * @author Chengyuan Zhao
 */
public class SpannerTableExporterTests {

  private StandardServiceRegistry registry;

  private JDBCMockObjectFactory jdbcMockObjectFactory;

  /**
   * Set up the metadata for Hibernate to generate schema statements.
   */
  @Before
  public void setup() {
    this.jdbcMockObjectFactory = new JDBCMockObjectFactory();
    this.jdbcMockObjectFactory.registerMockDriver();
    this.jdbcMockObjectFactory.getMockDriver()
        .setupConnection(this.jdbcMockObjectFactory.getMockConnection());

    this.registry = new StandardServiceRegistryBuilder()
        .applySetting("hibernate.dialect", SpannerDialect.class.getName())
        // must NOT set a driver class name so that Hibernate will use java.sql.DriverManager
        // and discover the only mock driver we have set up.
        .applySetting("hibernate.connection.url", "unused")
        .applySetting("hibernate.connection.username", "unused")
        .applySetting("hibernate.connection.password", "unused")
        .applySetting("hibernate.hbm2ddl.auto", "create")
        .build();
  }

  @Test
  public void generateDropStringsTest() throws IOException {
    Metadata metadata =
        new MetadataSources(this.registry)
            .addAnnotatedClass(TestEntity.class)
            .buildMetadata();

    Session session = metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    session.close();

    List<String> sqlStrings = this.jdbcMockObjectFactory.getMockConnection()
        .getStatementResultSetHandler()
        .getExecutedStatements()
        .stream()
        .filter(statement -> statement.startsWith("drop"))
        .collect(Collectors.toList());

    assertThat(sqlStrings)
        .containsExactlyInAnyOrder("drop table TestEntity_stringList", "drop table `test_table`");
  }

  @Test
  public void generateDeleteStringsWithIndices() throws IOException {
    Metadata metadata =
        new MetadataSources(this.registry)
            .addAnnotatedClass(Employee.class)
            .buildMetadata();

    Session session = metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    session.close();

    List<String> sqlStrings = this.jdbcMockObjectFactory.getMockConnection()
        .getStatementResultSetHandler()
        .getExecutedStatements()
        .stream()
        .filter(statement -> statement.startsWith("drop"))
        .collect(Collectors.toList());

    assertThat(sqlStrings)
        .containsExactly("drop index name_index", "drop table Employee", "drop table hibernate_sequence");
  }

  @Test
  public void generateCreateStringsTest() throws IOException {
    Metadata metadata =
        new MetadataSources(this.registry)
            .addAnnotatedClass(TestEntity.class)
            .buildMetadata();

    Session session = metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    session.close();

    List<String> sqlStrings = this.jdbcMockObjectFactory.getMockConnection()
        .getStatementResultSetHandler()
        .getExecutedStatements()
        .stream()
        .filter(statement -> statement.startsWith("create"))
        .collect(Collectors.toList());

    String expectedCreateString = "create table `test_table` (`boolColumn` BOOL,`ID1` " +
        "INT64 not null,id2 STRING(255) not null,longVal INT64 not null," +
        "stringVal STRING(255)) PRIMARY KEY (`ID1`,id2)";

    String expectedCollectionCreateString = "create table TestEntity_stringList " +
        "(stringList STRING(255),`TestEntity_ID1` INT64 not null," +
        "TestEntity_id2 STRING(255) not null) PRIMARY KEY (stringList,`TestEntity_ID1`,TestEntity_id2)";

    assertThat(sqlStrings)
        .containsExactlyInAnyOrder(expectedCreateString, expectedCollectionCreateString);
  }

  @Test
  public void generateCreateStringsEmptyEntityTest() {
    assertThatThrownBy(() -> {
      new MetadataSources(this.registry)
          .addAnnotatedClass(EmptyEntity.class)
          .buildMetadata();
    })
        .isInstanceOf(AnnotationException.class)
        .hasMessage(
            "No identifier specified for entity: "
                + "com.google.cloud.spanner.hibernate.SpannerTableExporterTests$EmptyEntity");
  }

  @Test
  public void generateCreateStringsNoPkEntityTest() {
    assertThatThrownBy(() -> {
      new MetadataSources(this.registry)
          .addAnnotatedClass(NoPkEntity.class)
          .buildMetadata();
    })
        .isInstanceOf(AnnotationException.class)
        .hasMessage(
            "No identifier specified for entity: "
                + "com.google.cloud.spanner.hibernate.SpannerTableExporterTests$NoPkEntity");
  }

  @Entity
  class EmptyEntity {
    // Intentionally empty
  }

  @Entity
  class NoPkEntity {

    // Intentionally no primary key annotated
    String value;
  }
}
