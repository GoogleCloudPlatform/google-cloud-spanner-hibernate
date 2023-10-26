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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThrows;

import com.google.cloud.spanner.hibernate.entities.Employee;
import com.google.cloud.spanner.hibernate.entities.TestEntity;
import com.mockrunner.mock.jdbc.JDBCMockObjectFactory;
import com.mockrunner.mock.jdbc.MockConnection;
import jakarta.persistence.Entity;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.hibernate.AnnotationException;
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

  private Metadata metadata;

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
        .applySetting("hibernate.connection.url", "unused")
        .build();

    this.metadata =
        new MetadataSources(this.registry).addAnnotatedClass(TestEntity.class).buildMetadata();
  }

  @Test
  public void generateDropStringsTest() throws IOException, SQLException {

    this.connection.setMetaData(MockJdbcUtils.metaDataBuilder()
        .setTables("test_table", "TestEntity_stringList")
        .build());

    String testFileName = UUID.randomUUID().toString();
    new SchemaExport().setOutputFile(testFileName)
        .drop(EnumSet.of(TargetType.STDOUT, TargetType.SCRIPT), this.metadata);
    File scriptFile = new File(testFileName);
    scriptFile.deleteOnExit();
    List<String> statements = Files.readAllLines(scriptFile.toPath());
    assertThat(statements)
        .containsExactly(
            "START BATCH DDL;",
            "drop table `TestEntity_stringList`;",
            "drop table `test_table`;",
            "RUN BATCH;");
  }

  @Test
  public void generateDeleteStringsWithIndices() throws IOException, SQLException {
    SpannerDialect.disableSpannerSequences();
    try {
      this.connection.setMetaData(MockJdbcUtils.metaDataBuilder()
          .setTables("Employee", "hibernate_sequence")
          .setIndices("name_index")
          .build());

      Metadata employeeMetadata =
          new MetadataSources(this.registry).addAnnotatedClass(Employee.class).buildMetadata();
      String testFileName = UUID.randomUUID().toString();
      new SchemaExport().setOutputFile(testFileName)
          .drop(EnumSet.of(TargetType.STDOUT, TargetType.SCRIPT), employeeMetadata);
      File scriptFile = new File(testFileName);
      scriptFile.deleteOnExit();
      List<String> statements = Files.readAllLines(scriptFile.toPath());

      assertThat(statements).containsExactly(
          "START BATCH DDL;",
          "drop index name_index;",
          "drop table Employee;",
          "drop table hibernate_sequence;",
          "RUN BATCH;");
    } finally {
      SpannerDialect.enableSpannerSequences();
    }
  }

  @Test
  public void generateDeleteStringsWithIndices_withSequencesEnabled()
      throws IOException, SQLException {
    this.connection.setMetaData(MockJdbcUtils.metaDataBuilder()
        .setTables("Employee")
        .setIndices("name_index")
        .build());

    Metadata employeeMetadata =
        new MetadataSources(this.registry).addAnnotatedClass(Employee.class).buildMetadata();
    String testFileName = UUID.randomUUID().toString();
    new SchemaExport().setOutputFile(testFileName)
        .drop(EnumSet.of(TargetType.STDOUT, TargetType.SCRIPT), employeeMetadata);
    File scriptFile = new File(testFileName);
    scriptFile.deleteOnExit();
    List<String> statements = Files.readAllLines(scriptFile.toPath());

    assertThat(statements).containsExactly(
        "START BATCH DDL;",
        "drop index name_index;",
        "drop table Employee;",
        "drop sequence hibernate_sequence;",
        "RUN BATCH;");
  }

  @Test
  public void omitCreatingPreexistingTables() throws IOException, SQLException {
    SpannerDialect.disableSpannerSequences();
    try {
      this.connection.setMetaData(MockJdbcUtils.metaDataBuilder()
          .setTables("Employee")
          .build());

      Metadata employeeMetadata =
          new MetadataSources(this.registry).addAnnotatedClass(Employee.class).buildMetadata();
      String testFileName = UUID.randomUUID().toString();
      new SchemaExport().setOutputFile(testFileName)
          .createOnly(EnumSet.of(TargetType.STDOUT, TargetType.SCRIPT), employeeMetadata);
      File scriptFile = new File(testFileName);
      scriptFile.deleteOnExit();
      List<String> statements = Files.readAllLines(scriptFile.toPath());

      assertThat(statements).containsExactly(
          // This omits creating the Employee table since it is declared to exist in metadata.
          "START BATCH DDL;",
          "create table hibernate_sequence (next_val INT64) PRIMARY KEY ();",
          "create index name_index on Employee (name);",
          "alter table Employee add constraint FKiralam2duuhr33k8a10aoc2t6 "
              + "foreign key (manager_id) references Employee (id);",
          "RUN BATCH;",
          "INSERT INTO hibernate_sequence (next_val) VALUES(1);"
      );
    } finally {
      SpannerDialect.enableSpannerSequences();
    }
  }

  @Test
  public void omitCreatingPreexistingTables_withSequencesEnabled()
      throws IOException, SQLException {
    this.connection.setMetaData(MockJdbcUtils.metaDataBuilder()
        .setTables("Employee")
        .build());

    Metadata employeeMetadata =
        new MetadataSources(this.registry).addAnnotatedClass(Employee.class).buildMetadata();
    String testFileName = UUID.randomUUID().toString();
    new SchemaExport().setOutputFile(testFileName)
        .createOnly(EnumSet.of(TargetType.STDOUT, TargetType.SCRIPT), employeeMetadata);
    File scriptFile = new File(testFileName);
    scriptFile.deleteOnExit();
    List<String> statements = Files.readAllLines(scriptFile.toPath());

    assertThat(statements).containsExactly(
        // This omits creating the Employee table since it is declared to exist in metadata.
        "START BATCH DDL;",
        "create sequence hibernate_sequence options(sequence_kind=\"bit_reversed_positive\");",
        "create index name_index on Employee (name);",
        "alter table Employee add constraint FKiralam2duuhr33k8a10aoc2t6 "
            + "foreign key (manager_id) references Employee (id);",
        "RUN BATCH;"
    );
  }

  @Test
  public void generateCreateStringsTest() throws IOException {
    String testFileName = UUID.randomUUID().toString();
    new SchemaExport().setOutputFile(testFileName)
        .createOnly(EnumSet.of(TargetType.STDOUT, TargetType.SCRIPT), this.metadata);
    File scriptFile = new File(testFileName);
    scriptFile.deleteOnExit();
    List<String> statements = Files.readAllLines(scriptFile.toPath());

    // The types in the following string need to be updated when SpannerDialect
    // implementation maps types.
    String expectedCreateString = "create table `test_table` (`ID1` int64 not null,id2"
        + " string(255) not null,`boolColumn` bool,longVal int64 not null,stringVal"
        + " string(255)) PRIMARY KEY (`ID1`,id2);";

    String expectedCollectionCreateString = "create table `TestEntity_stringList` "
        + "(`TestEntity_ID1` int64 not null,`TestEntity_id2` string(255) not null,"
        + "stringList string(255)) PRIMARY KEY (`TestEntity_ID1`,`TestEntity_id2`,stringList);";

    String foreignKeyString =
        "alter table `TestEntity_stringList` add constraint FK2is6fwy3079dmfhjot09x5och "
            + "foreign key (`TestEntity_ID1`, `TestEntity_id2`) "
            + "references `test_table` (`ID1`, id2);";

    assertThat(statements.get(0)).isEqualTo("START BATCH DDL;");
    assertThat(statements.subList(1, 4))
        .containsExactlyInAnyOrder(
            expectedCreateString, expectedCollectionCreateString, foreignKeyString);
    assertThat(statements.get(4)).isEqualTo("RUN BATCH;");
  }

  @Test
  public void generateCreateStringsEmptyEntityTest() {
    assertThatThrownBy(() -> {
      Metadata metadata = new MetadataSources(this.registry)
          .addAnnotatedClass(EmptyEntity.class)
          .buildMetadata();
      new SchemaExport()
          .setOutputFile("unused")
          .createOnly(EnumSet.of(TargetType.STDOUT, TargetType.SCRIPT), metadata);
    })
        .isInstanceOf(AnnotationException.class)
        .hasMessage(
            "Entity 'com.google.cloud.spanner.hibernate.SpannerTableExporterTests$EmptyEntity' "
                + "has no identifier (every '@Entity' class must declare or inherit at least "
                + "one '@Id' or '@EmbeddedId' property)");
  }

  @Test
  public void generateCreateStringsNoPkEntityTest() {
    assertThatThrownBy(() -> {
      Metadata metadata = new MetadataSources(this.registry)
          .addAnnotatedClass(NoPkEntity.class)
          .buildMetadata();

      new SchemaExport()
          .setOutputFile("unused")
          .createOnly(EnumSet.of(TargetType.STDOUT, TargetType.SCRIPT), metadata);
    })
        .isInstanceOf(AnnotationException.class)
        .hasMessage(
            "Entity 'com.google.cloud.spanner.hibernate.SpannerTableExporterTests$NoPkEntity' "
                + "has no identifier (every '@Entity' class must declare or inherit at least "
                + "one '@Id' or '@EmbeddedId' property)");
  }

  @Test
  public void testAddStatementAfterDdlBatchFailsWithNoBatch() {
    Metadata metadata =
        new MetadataSources(this.registry).addAnnotatedClass(Employee.class).buildMetadata();
    assertThrows(
        IllegalStateException.class,
        () ->
            SpannerTableExporter.addStatementAfterDdlBatch(
                metadata, new String[] {"insert into foo"}));
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
