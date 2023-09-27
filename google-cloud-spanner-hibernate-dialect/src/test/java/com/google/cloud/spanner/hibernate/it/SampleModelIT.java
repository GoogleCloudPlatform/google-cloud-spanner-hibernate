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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.cloud.spanner.IntegrationTest;
import com.google.cloud.spanner.hibernate.it.model.Album;
import com.google.cloud.spanner.hibernate.it.model.Singer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Category(IntegrationTest.class)
@RunWith(JUnit4.class)
public class SampleModelIT {

  private static final HibernateIntegrationTestEnv TEST_ENV = new HibernateIntegrationTestEnv();

  private static SessionFactory sessionFactory;

  @BeforeClass
  public static void createDataDatabase() {
    TEST_ENV.createDatabase(ImmutableList.of());
    sessionFactory = TEST_ENV.createTestHibernateConfig(
        ImmutableList.of(Singer.class, Album.class),
        ImmutableMap.of("hibernate.hbm2ddl.auto", "update")).buildSessionFactory();
    try (Session session = sessionFactory.openSession()) {
      final Transaction transaction = session.beginTransaction();
      session.save(new Singer("Peter", "Allison"));
      session.save(new Singer("Alice", "Peterson"));
      transaction.commit();
    }
  }

  @AfterClass
  public static void cleanup() {
    if (sessionFactory != null) {
      sessionFactory.close();
    }
    TEST_ENV.cleanup();
  }

  @Test
  public void testGetSinger() {
    try (Session session = sessionFactory.openSession()) {
      // The id generator should start counting at 50000 and the values should be bit-reversed.
      assertNull(session.get(Singer.class, 1L));
      assertNull(session.get(Singer.class, 50000L));
      Singer singer = session.get(Singer.class, Long.reverse(50000L));
      assertNotNull(singer);
      assertEquals("Peter", singer.getFirstName());
      assertEquals("Allison", singer.getLastName());
      assertEquals("Peter Allison", singer.getFullName());
      assertNull(singer.getBirthDate());
      assertNotNull(singer.getCreatedAt());
      assertNotNull(singer.getUpdatedAt());
    }
  }

  @Test
  public void testSaveSinger() {
    try (Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      Singer singer1 = new Singer("singer1", "singer1");
      singer1.setBirthDate(LocalDate.of(1998, 10, 12));
      assertNotNull(session.save(singer1));
      session.flush();
      assertEquals("singer1 singer1", singer1.getFullName());
      assertEquals(LocalDate.of(1998, 10, 12), singer1.getBirthDate());

      Singer singer2 = new Singer(null, "singer2");
      assertNotNull(session.save(singer2));
      session.flush();
      assertEquals("singer2", singer2.getFullName());

      Singer singer3 = new Singer("singer3", null);
      assertNotNull(session.save(singer3));
      session.flush();
      assertEquals("singer3", singer3.getFullName());

      // This should not be allowed, as either firstName or lastName must be not null, but Hibernate
      // does not generate the check constraint.
      // TODO: Revisit this in Hibernate 6.
      Singer singer4 = new Singer(null, null);
      session.save(singer4);
      session.flush();
      assertNull(singer4.getFullName());
    }
  }

}
