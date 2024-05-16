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

package com.google.cloud.spanner.hibernate.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.cloud.spanner.IntegrationTest;
import com.google.common.collect.ImmutableList;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests basic functionality end-to-end for the Hibernate dialect. */
@Category(IntegrationTest.class)
@RunWith(JUnit4.class)
public class BasicIntegrationTest {

  private static final HibernateIntegrationTestEnv TEST_ENV = new HibernateIntegrationTestEnv();

  @BeforeClass
  public static void setup() {
    TEST_ENV.createDatabase(
        ImmutableList.of("create table TestEntity (id int64, value string(max)) primary key (id)"));
  }

  @AfterClass
  public static void cleanup() {
    TEST_ENV.cleanup();
  }

  @Test
  public void testCrud() {
    try (SessionFactory factory =
            TEST_ENV
                .createTestHibernateConfig(ImmutableList.of(TestEntity.class))
                .buildSessionFactory();
        Session session = factory.openSession()) {
      // Insert a row.
      runWithTransactionAndClearSession(session, () -> session.save(new TestEntity(1L, "One")));

      // Verify that the row was actually written.
      TestEntity entity = session.get(TestEntity.class, 1L);
      assertEquals(1L, entity.id.longValue());
      assertEquals("One", entity.value);

      // Update the row and verify that the update is written to the database.
      entity.value = "One - Updated";
      runWithTransactionAndClearSession(session, () -> session.update(entity));
      TestEntity updatedEntity = session.get(TestEntity.class, 1L);
      assertEquals("One - Updated", updatedEntity.value);
      session.clear();

      // Delete the row.
      runWithTransactionAndClearSession(session, () -> session.delete(entity));
      assertNull(session.get(TestEntity.class, 1L));
    }
  }

  private void runWithTransactionAndClearSession(Session session, Runnable runnable) {
    Transaction transaction = session.beginTransaction();
    runnable.run();
    transaction.commit();
    // Clear the Hibernate session to ensure that subsequence actions will read data from the
    // database and not from the session cache.
    session.clear();
  }

  @Table(name = "TestEntity")
  @Entity
  static class TestEntity {

    @Id private Long id;

    @Column private String value;

    protected TestEntity() {}

    TestEntity(long id, String value) {
      this.id = id;
      this.value = value;
    }
  }
}
