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

import com.google.cloud.spanner.hibernate.util.TestEntity;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Tests for SpannerTableExporter.
 *
 * @author Chengyuan Zhao
 */
public class SpannerTableExporterTests {

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
    List<String> statements = Files.readAllLines(scriptFile.toPath());
    try {
      assertEquals("test_placeholder", statements.get(0));
    } finally {
      scriptFile.delete();
    }
  }

  @Test
  public void generateCreateStringsTest() throws IOException {
    String testFileName = UUID.randomUUID().toString();
    new SchemaExport().setOutputFile(testFileName)
        .create(EnumSet.of(TargetType.STDOUT, TargetType.SCRIPT), this.metadata);
    File scriptFile = new File(testFileName);
    List<String> statements = Files.readAllLines(scriptFile.toPath());
    try {
      assertEquals("test_placeholder", statements.get(0));
    } finally {
      scriptFile.delete();
    }
  }
}
