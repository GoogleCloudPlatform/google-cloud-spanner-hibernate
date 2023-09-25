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

import static com.google.cloud.spanner.testing.EmulatorSpannerHelper.isUsingEmulator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import com.google.cloud.spanner.IntegrationTest;
import com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory.JdbcAbortedDueToConcurrentModificationException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.cfg.Environment;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for bit-reversed sequences. */
@Category(IntegrationTest.class)
@RunWith(JUnit4.class)
public class BitReversedSequenceIT {

  private static final HibernateIntegrationTestEnv TEST_ENV = new HibernateIntegrationTestEnv();
  
  interface SequenceEntity {
    long getId();
    
    String getName();
    
    static <T extends SequenceEntity> T create(Class<T> entityClass, String name) throws Exception {
      return entityClass.getConstructor(String.class).newInstance(name);
    }
  }
  
  @Table(name = "default_sequence_entity")
  @Entity
  static class DefaultSequenceEntity implements SequenceEntity {
    
    @Id
    @GeneratedValue
    private long id;
    
    private String name;
    
    public DefaultSequenceEntity() {}
    
    public DefaultSequenceEntity(String name) {
      this.name = name;
    }
    
    public long getId() {
      return id;
    }
    
    public String getName() {
      return name;
    }
  }
  
  @Table(name = "pooled_sequence_entity")
  @Entity
  static class PooledSequenceEntity implements SequenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
                    generator = "pooled_sequence_entity_generator")
    @GenericGenerator(name = "pooled_sequence_entity_generator",
        strategy = "com.google.cloud.spanner.hibernate.PooledBitReversedSequenceStyleGenerator",
        parameters = {
            @Parameter(name = "sequence_name", value = "pooled_sequence"),
            @Parameter(name = "increment_size", value = "200"),
            @Parameter(name = "initial_value", value = "5000"),
            @Parameter(name = "exclude_range", value = "[1,1000]")})
    private long id;

    private String name;

    public PooledSequenceEntity() {}
    
    public PooledSequenceEntity(String name) {
      this.name = name;
    }

    public long getId() {
      return id;
    }

    public String getName() {
      return name;
    }
  }

  /** Creates a test database and generates the schema from the entities. */
  @BeforeClass
  public static void setup() {
    assumeFalse("bit-reversed sequences are not yet supported on the emulator", isUsingEmulator());
    
    TEST_ENV.createDatabase(ImmutableList.of());
    // Generate the database schema from th entity model.
    try (SessionFactory ignore = TEST_ENV.createTestHibernateConfig(
        ImmutableList.of(DefaultSequenceEntity.class, PooledSequenceEntity.class),
        ImmutableMap.of(Environment.HBM2DDL_AUTO, "create-only")).buildSessionFactory()) {
      // do nothing, just make sure the schema is generated.
    }
  }

  /** Drops the test database. */
  @AfterClass
  public static void cleanup() {
    TEST_ENV.cleanup();
  }
  
  @Test
  public void testDefaultSequenceEntity() throws Exception {
    testSequenceEntity(DefaultSequenceEntity.class);
  }

  @Test
  public void testPooledSequenceEntity() throws Exception {
    testSequenceEntity(PooledSequenceEntity.class);
  }
  
  private <T extends SequenceEntity> void testSequenceEntity(Class<T> entityClass)
      throws Exception {
    final int numRows = 300;
    try (SessionFactory factory = TEST_ENV.createTestHibernateConfig(
        ImmutableList.of(entityClass),
        ImmutableMap.of(Environment.STATEMENT_BATCH_SIZE, "50")).buildSessionFactory();
        Session session = factory.openSession()) {
      Transaction transaction = null;
      while (true) {
        try {
          transaction = session.beginTransaction();
          for (int i = 0; i < numRows; i++) {
            assertTrue((long) session.save(SequenceEntity.create(entityClass, "test " + i)) > 0L);
          }
          transaction.commit();
          break;
        } catch (HibernateException hibernateException) {
          // TODO: Remove this when the Connection API supports the hint
          //       /* spanner.ignore_during_internal_retry=true */
          if (transaction != null) {
            transaction.rollback();
          }
          if (hibernateException.getCause() 
              instanceof JdbcAbortedDueToConcurrentModificationException) {
            continue;
          }
          throw hibernateException;
        }
      }
        
      // Clear the session and load the entities back into memory.
      final Transaction readTransaction = session.beginTransaction();
      CriteriaBuilder builder = session.getCriteriaBuilder();
      CriteriaQuery<T> query = builder.createQuery(entityClass);
      Root<T> root = query.from(entityClass);
      CriteriaQuery<T> all = query.select(root);

      TypedQuery<T> allQuery = session.createQuery(all);
      List<T> rows = allQuery.getResultList();

      assertEquals(numRows, rows.size());
      for (T row : rows) {
        assertTrue(row.getId() > 0L);
        assertNotNull(row.getName());
      }
      readTransaction.commit();
    }
  }

}
