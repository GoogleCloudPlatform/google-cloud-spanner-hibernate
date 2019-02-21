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

import static org.junit.Assert.assertEquals;

import com.google.cloud.spanner.hibernate.util.TestEntity;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import org.hibernate.AnnotationException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for SpannerTableExporter.
 *
 * @author Chengyuan Zhao
 */
public class SpannerTableExporterTests {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Metadata metadata;

  private StandardServiceRegistry registry;

  /**
   * Set up the metadata for Hibernate to generate schema statements.
   */
  @Before
  public void setup() {
    this.registry = new StandardServiceRegistryBuilder()
        .applySetting("hibernate.dialect", SpannerDialect.class.getName()).build();
    this.metadata =
        new MetadataSources(this.registry).addAnnotatedClass(TestEntity.class).buildMetadata();
  }

  @Test
  public void generateDropStringsTest() throws IOException {
    String testFileName = UUID.randomUUID().toString();
    new SchemaExport().setOutputFile(testFileName)
        .drop(EnumSet.of(TargetType.STDOUT, TargetType.SCRIPT), this.metadata);
    File scriptFile = new File(testFileName);
    scriptFile.deleteOnExit();
    List<String> statements = Files.readAllLines(scriptFile.toPath());
    assertEquals("drop table `test_table`", statements.get(0));
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
    assertEquals("create table `test_table` "
        + "(`ID1` bigint not null,id2 varchar(255) not null,"
        + "`boolColumn` boolean,longVal bigint not null,stringVal varchar(255)) "
        + "PRIMARY KEY (`ID1`,id2)", statements.get(0));
  }

  @Test
  public void generateCreateStringsEmptyEntityTest() {
    this.expectedException.expect(AnnotationException.class);
    this.expectedException.expectMessage("No identifier specified for entity:");
    new SchemaExport().setOutputFile("unused")
        .createOnly(EnumSet.of(TargetType.STDOUT, TargetType.SCRIPT),
            new MetadataSources(this.registry).addAnnotatedClass(EmptyEntity.class)
                .buildMetadata());
  }

  @Test
  public void generateCreateStringsNoPkEntityTest() {
    this.expectedException.expect(AnnotationException.class);
    this.expectedException.expectMessage("No identifier specified for entity:");
    new SchemaExport().setOutputFile("unused")
        .createOnly(EnumSet.of(TargetType.STDOUT, TargetType.SCRIPT),
            new MetadataSources(this.registry).addAnnotatedClass(NoPkEntity.class).buildMetadata());
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
