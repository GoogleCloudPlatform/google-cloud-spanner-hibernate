/*
 * Copyright 2019-2025 Google LLC
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

package com.google.cloud.spanner.hibernate.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.cloud.spanner.IntegrationTest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration test for Named Schema support. */
@Category(IntegrationTest.class)
@RunWith(JUnit4.class)
public class NamedSchemaIT {

  private static final HibernateIntegrationTestEnv TEST_ENV = new HibernateIntegrationTestEnv();

  @BeforeClass
  public static void setup() {
    TEST_ENV.createDatabase(ImmutableList.of());
  }

  @AfterClass
  public static void cleanup() {
    TEST_ENV.cleanup();
  }

  /**
   * Tests the combination of a configured `default_schema` AND explicit `@Table(schema=...)`
   * annotations.
   *
   * <p>Expected behavior:
   *
   * <ul>
   *   <li>Entities without a schema annotation are created in the `default_schema`.
   *   <li>Entities with a schema annotation are created in their specific schema, ignoring the
   *       default.
   * </ul>
   */
  @Test
  public void testDefaultAndExplicitSchemaCombination() {
    final String defaultSchema = "store_def";

    try (SessionFactory factory =
            TEST_ENV
                .createTestHibernateConfig(
                    ImmutableList.of(DefaultItem.class, CatalogItem.class, UserProfile.class),
                    ImmutableMap.of(
                        AvailableSettings.HBM2DDL_AUTO, "create",
                        AvailableSettings.SHOW_SQL, "true",
                        AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS, "true",
                        AvailableSettings.DEFAULT_SCHEMA, defaultSchema))
                .buildSessionFactory();
        Session session = factory.openSession()) {

      // 1. Insert data for all schemas in a single transaction
      Transaction tx = session.beginTransaction();

      DefaultItem item = new DefaultItem("Hammer");
      session.persist(item);

      CatalogItem catalogEntry = new CatalogItem("Hardware-001");
      session.persist(catalogEntry);

      UserProfile user = new UserProfile("jdoe");
      session.persist(user);

      tx.commit();
      session.clear();

      Transaction readTx = session.beginTransaction();

      DefaultItem loadedItem = session.find(DefaultItem.class, item.getId());
      assertNotNull(
          "Item should be found in default schema table (" + defaultSchema + ".items)", loadedItem);
      assertEquals("Hammer", loadedItem.getName());

      CatalogItem loadedCatalog = session.find(CatalogItem.class, catalogEntry.getId());
      assertNotNull("Catalog item should be found in 'catalog' schema", loadedCatalog);
      assertEquals("Hardware-001", loadedCatalog.getSku());

      UserProfile loadedUser = session.find(UserProfile.class, user.getId());
      assertNotNull("User should be found in 'users' schema", loadedUser);
      assertEquals("jdoe", loadedUser.getUsername());

      readTx.commit();
    }
  }

  /**
   * Tests explicit `@Table(schema=...)` support when NO default schema is configured.
   *
   * <p>Expected behavior:
   *
   * <ul>
   *   <li>Entities with a schema annotation are created in their specific schema.
   *   <li>Entities without a schema annotation are created in the database's public/default
   *       namespace.
   * </ul>
   */
  @Test
  public void testExplicitSchemaOnly() {
    try (SessionFactory factory =
            TEST_ENV
                .createTestHibernateConfig(
                    ImmutableList.of(DefaultItem.class, UserProfile.class),
                    ImmutableMap.of(
                        AvailableSettings.HBM2DDL_AUTO, "create",
                        AvailableSettings.SHOW_SQL, "true",
                        AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS, "true"))
                .buildSessionFactory();
        Session session = factory.openSession()) {

      Transaction tx = session.beginTransaction();
      DefaultItem publicItem = new DefaultItem("Wrench");
      session.persist(publicItem);

      UserProfile schemaUser = new UserProfile("admin");
      session.persist(schemaUser);
      tx.commit();
      session.clear();

      Transaction readTx = session.beginTransaction();
      assertNotNull(session.find(DefaultItem.class, publicItem.getId()));
      assertNotNull(session.find(UserProfile.class, schemaUser.getId()));
      readTx.commit();
    }
  }

  /**
   * Tests that indexes defined on entities in a named schema are correctly created.
   *
   * <p>This verifies that the CREATE INDEX statement is schema-qualified (e.g. {@code CREATE INDEX
   * schema.index ...}) if required by the dialect.
   */
  @Test
  public void testIndexesInNamedSchema() {
    try (SessionFactory factory =
            TEST_ENV
                .createTestHibernateConfig(
                    ImmutableList.of(IndexedEntity.class),
                    ImmutableMap.of(
                        AvailableSettings.HBM2DDL_AUTO, "create",
                        AvailableSettings.SHOW_SQL, "true",
                        AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS, "true"))
                .buildSessionFactory();
        Session session = factory.openSession()) {

      Transaction tx = session.beginTransaction();
      IndexedEntity entity = new IndexedEntity("val-123");
      session.persist(entity);
      tx.commit();
    }
  }

  // Test Entities

  /**
   * Entity with NO schema annotation. Should inherit 'hibernate.default_schema' if set, otherwise
   * use database default.
   */
  @Entity
  @Table(name = "items")
  static class DefaultItem {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String name;

    public DefaultItem() {}

    public DefaultItem(String name) {
      this.name = name;
    }

    public UUID getId() {
      return id;
    }

    public String getName() {
      return name;
    }
  }

  /** Entity with explicit schema 'catalog'. Should always use 'catalog.catalog_items'. */
  @Entity
  @Table(name = "catalog_items", schema = "catalog")
  static class CatalogItem {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String sku;

    public CatalogItem() {}

    public CatalogItem(String sku) {
      this.sku = sku;
    }

    public UUID getId() {
      return id;
    }

    public String getSku() {
      return sku;
    }
  }

  /** Entity with explicit schema 'users'. Should always use 'users.profiles'. */
  @Entity
  @Table(name = "profiles", schema = "users")
  static class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true)
    private String username;

    public UserProfile() {}

    public UserProfile(String username) {
      this.username = username;
    }

    public UUID getId() {
      return id;
    }

    public String getUsername() {
      return username;
    }
  }

  /** Entity mapped to 'logistics' schema with an index. */
  @Entity
  @Table(
      name = "indexed_table",
      schema = "logistics",
      indexes = @Index(name = "idx_value", columnList = "value"))
  static class IndexedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String value;

    public IndexedEntity() {}

    public IndexedEntity(String value) {
      this.value = value;
    }

    public UUID getId() {
      return id;
    }

    public void setId(UUID id) {
      this.id = id;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }
}
