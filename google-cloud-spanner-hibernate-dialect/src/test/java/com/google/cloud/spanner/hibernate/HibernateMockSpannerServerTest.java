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

import static com.google.cloud.spanner.hibernate.AbstractSchemaGenerationMockServerTest.GET_SEQUENCES_METADATA;
import static com.google.cloud.spanner.hibernate.AbstractSchemaGenerationMockServerTest.GET_SEQUENCES_STATEMENT;
import static com.google.cloud.spanner.hibernate.AbstractSchemaGenerationMockServerTest.createSequenceRow;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.hibernate.entities.Album;
import com.google.cloud.spanner.hibernate.entities.IdentityEntity;
import com.google.cloud.spanner.hibernate.entities.Singer;
import com.google.cloud.spanner.hibernate.hints.Hints;
import com.google.cloud.spanner.hibernate.hints.ReplaceQueryPartsHint.ReplaceMode;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ResultSet;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.ResultSetStats;
import com.google.spanner.v1.RollbackRequest;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.Type;
import com.google.spanner.v1.TypeCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.query.Query;
import org.junit.Test;

/** Tests Hibernate configuration using an in-memory mock Spanner server. */
public class HibernateMockSpannerServerTest extends AbstractMockSpannerServerTest {

  private static final ImmutableList<Class<?>> ENTITY_CLASSES =
      ImmutableList.of(Singer.class, Album.class);

  static ResultSet createBitReversedSequenceResultSet(long startValue, long endValue) {
    return ResultSet.newBuilder()
        .setMetadata(
            ResultSetMetadata.newBuilder()
                .setRowType(
                    StructType.newBuilder()
                        .addFields(
                            Field.newBuilder()
                                .setName("n")
                                .setType(Type.newBuilder().setCode(TypeCode.INT64).build())
                                .build())
                        .build())
                .build())
        .addAllRows(
            LongStream.range(startValue, endValue)
                .map(HibernateMockSpannerServerTest::reverse)
                .mapToObj(
                    id ->
                        ListValue.newBuilder()
                            .addValues(
                                Value.newBuilder().setStringValue(String.valueOf(id)).build())
                            .build())
                .collect(Collectors.toList()))
        .build();
  }

  @Test
  public void testHibernateGetSinger() {
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.newBuilder("select s1_0.id,s1_0.name from Singer s1_0 where s1_0.id=@p1")
                .bind("p1")
                .to(1L)
                .build(),
            createSingerResultSet(ImmutableList.of(new Singer(1L, "test")))));

    try (SessionFactory sessionFactory =
            createTestHibernateConfig(ENTITY_CLASSES).buildSessionFactory();
        Session session = sessionFactory.openSession()) {
      Singer singer = session.get(Singer.class, 1L);
      assertEquals(1L, singer.getId());
    }
  }

  @Test
  public void testHibernateSaveSinger() {
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of("select next_val as id_val from singerId for update"),
            com.google.spanner.v1.ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(
                            StructType.newBuilder()
                                .addFields(
                                    Field.newBuilder()
                                        .setName("id_val")
                                        .setType(Type.newBuilder().setCode(TypeCode.INT64).build())
                                        .build())
                                .build())
                        .build())
                .addRows(
                    ListValue.newBuilder()
                        .addValues(Value.newBuilder().setStringValue("50000").build())
                        .build())
                .build()));
    mockSpanner.putStatementResult(
        StatementResult.update(
            Statement.newBuilder("update singerId set next_val= @p1 where next_val=@p2")
                .bind("p1")
                .to(51000L)
                .bind("p2")
                .to(50000L)
                .build(),
            1L));

    try (SessionFactory sessionFactory =
            createTestHibernateConfig(ENTITY_CLASSES).buildSessionFactory();
        Session session = sessionFactory.openSession()) {
      long id = (long) session.save(new Singer());
      assertEquals(Long.reverse(50000L), id);
    }
  }

  @Test
  public void testHibernateSaveSinger_skipsExcludedRange() {
    // singerId will skip the ranges [1,1000] [10000,20000]
    // Long.reverse(3422735716801576960) == 500

    // The initial value will be skipped and instead (initial+1) will be used:
    // Long.reverse(3422735716801576961) == -9223372036854775308
    long initialValue = 3422735716801576960L;
    long expectedId = Long.reverse(initialValue + 1L);
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of("select next_val as id_val from singerId for update"),
            com.google.spanner.v1.ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(
                            StructType.newBuilder()
                                .addFields(
                                    Field.newBuilder()
                                        .setName("id_val")
                                        .setType(Type.newBuilder().setCode(TypeCode.INT64).build())
                                        .build())
                                .build())
                        .build())
                .addRows(
                    ListValue.newBuilder()
                        .addValues(
                            Value.newBuilder()
                                .setStringValue(String.valueOf(initialValue + 1000L - 1L))
                                .build())
                        .build())
                .build()));
    mockSpanner.putStatementResult(
        StatementResult.update(
            Statement.newBuilder("update singerId set next_val= @p1 where next_val=@p2")
                .bind("p1")
                .to(3422735716801578959L)
                .bind("p2")
                .to(3422735716801577959L)
                .build(),
            1L));

    try (SessionFactory sessionFactory =
            createTestHibernateConfig(ENTITY_CLASSES).buildSessionFactory();
        Session session = sessionFactory.openSession()) {

      long id = (long) session.save(new Singer());
      assertEquals(expectedId, id);
    }
  }

  @Test
  public void testNonPooledBitReversedSequence() {
    mockSpanner.putStatementResult(
        StatementResult.query(
            GET_SEQUENCES_STATEMENT,
            ResultSet.newBuilder()
                .setMetadata(GET_SEQUENCES_METADATA)
                .addRows(createSequenceRow("test-entity_SEQ"))
                .build()));
    String getNextSequenceValueSql = "select get_next_sequence_value(sequence `test-entity-seq`)";

    long initialValue = 1L;
    String insertSql = "insert into `test-entity` (name,id) values (@p1,@p2)";
    for (int i = 0; i < 10; i++) {
      mockSpanner.putStatementResult(
          StatementResult.update(
              Statement.newBuilder(insertSql)
                  .bind("p1")
                  .to((String) null)
                  .bind("p2")
                  .to(reverse(initialValue + i))
                  .build(),
              1L));
    }
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of(getNextSequenceValueSql),
            createBitReversedSequenceResultSet(initialValue, initialValue + 1L)));

    try (SessionFactory sessionFactory =
            createTestHibernateConfig(
                    ImmutableList.of(NonPooledSequenceEntity.class),
                    ImmutableMap.of("hibernate.jdbc.batch_size", "100"))
                .buildSessionFactory();
        Session session = sessionFactory.openSession()) {
      final Transaction transaction = session.beginTransaction();
      for (int i = 0; i < 5; i++) {
        mockSpanner.putStatementResult(
            StatementResult.query(
                Statement.of(getNextSequenceValueSql),
                createBitReversedSequenceResultSet(initialValue + i, initialValue + i + 1L)));
        long id = (long) session.save(new NonPooledSequenceEntity());
        assertEquals(reverse(initialValue + i), id);
      }
      transaction.commit();
    }

    // There is one read/write transaction, as the default Hibernate SequenceStyle generator uses
    // the current transaction to fetch identifier values.
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));

    assertEquals(
        5,
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
            .filter(request -> request.getSql().equals(getNextSequenceValueSql))
            .count());
    ExecuteSqlRequest firstRequest =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
            .filter(request -> request.getSql().equals(getNextSequenceValueSql))
            .findFirst()
            .orElseThrow(Suppliers.ofInstance(new AssertionError("missing request")));
    assertTrue(firstRequest.hasTransaction());
    assertTrue(firstRequest.getTransaction().hasBegin());
    assertTrue(firstRequest.getTransaction().getBegin().hasReadWrite());

    // Entity insertion should use batch DML.
    assertEquals(
        0,
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
            .filter(request -> request.getSql().equals(insertSql))
            .count());
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class));
    ExecuteBatchDmlRequest batchDmlRequest =
        mockSpanner.getRequestsOfType(ExecuteBatchDmlRequest.class).get(0);
    assertTrue(batchDmlRequest.hasTransaction());
    assertTrue(batchDmlRequest.getTransaction().hasId());
    assertEquals(5, batchDmlRequest.getStatementsCount());
    long index = 0L;
    for (ExecuteBatchDmlRequest.Statement statement : batchDmlRequest.getStatementsList()) {
      assertEquals(insertSql, statement.getSql());
      assertEquals(
          String.valueOf(reverse(initialValue + index)),
          statement.getParams().getFieldsMap().get("p2").getStringValue());
      index++;
    }
  }

  @Test
  public void testHibernatePooledSequenceEntity_fetchesInBatches() {
    String getSequenceValuesSql =
        "/* spanner.force_read_write_transaction=true */ "
            + "/* spanner.ignore_during_internal_retry=true */ "
            + " select get_next_sequence_value(sequence pooled_sequence) AS n "
            + "from unnest(generate_array(1, 5))";

    long initialValue = 20000L;
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of(getSequenceValuesSql),
            createBitReversedSequenceResultSet(20000L, 20005L)));

    String insertSql = "insert into `test-entity` (name,id) values (@p1,@p2)";
    for (int i = 0; i < 10; i++) {
      mockSpanner.putStatementResult(
          StatementResult.update(
              Statement.newBuilder(insertSql)
                  .bind("p1")
                  .to((String) null)
                  .bind("p2")
                  .to(reverse(initialValue + i))
                  .build(),
              1L));
    }

    try (SessionFactory sessionFactory =
            createTestHibernateConfig(
                    ImmutableList.of(TestSequenceEntity.class),
                    ImmutableMap.of("hibernate.jdbc.batch_size", "100"))
                .buildSessionFactory();
        Session session = sessionFactory.openSession()) {
      final Transaction transaction = session.beginTransaction();
      // Insert 5 records. This should be possible with the first batch of identifiers.
      for (int i = 0; i < 5; i++) {
        long id = (long) session.save(new TestSequenceEntity());
        assertEquals(reverse(initialValue + i), id);
      }
      // Add a result for the next batch of sequence values.
      mockSpanner.putStatementResult(
          StatementResult.query(
              Statement.of(getSequenceValuesSql),
              createBitReversedSequenceResultSet(20005L, 20010L)));
      // Insert another 5 records. This should use the second batch of identifiers.
      for (int i = 0; i < 5; i++) {
        long id = (long) session.save(new TestSequenceEntity());
        assertEquals(reverse(initialValue + i + 5L), id);
      }
      transaction.commit();
    }

    // We should have three read/write transactions:
    // 1. Two separate transactions for getting the values from the bit-reversed sequence.
    // 2. A transaction corresponding to the Hibernate transaction.
    // The transactions for getting values from the bit-reversed sequence are rolled back instead
    // of committed, as this prevents the transaction from being aborted on the emulator.
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
    assertEquals(2, mockSpanner.countRequestsOfType(RollbackRequest.class));

    assertEquals(
        2,
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
            .filter(request -> request.getSql().equals(getSequenceValuesSql))
            .count());
    ExecuteSqlRequest getSequenceValuesRequest =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
            .filter(request -> request.getSql().equals(getSequenceValuesSql))
            .findFirst()
            .orElseThrow(Suppliers.ofInstance(new AssertionError("missing request")));
    assertTrue(getSequenceValuesRequest.hasTransaction());
    assertTrue(getSequenceValuesRequest.getTransaction().hasBegin());
    assertTrue(getSequenceValuesRequest.getTransaction().getBegin().hasReadWrite());

    // Entity insertion should use batch DML.
    assertEquals(
        0,
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
            .filter(request -> request.getSql().equals(insertSql))
            .count());
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class));
    ExecuteBatchDmlRequest batchDmlRequest =
        mockSpanner.getRequestsOfType(ExecuteBatchDmlRequest.class).get(0);
    assertTrue(batchDmlRequest.hasTransaction());
    assertTrue(batchDmlRequest.getTransaction().hasBegin());
    assertTrue(batchDmlRequest.getTransaction().getBegin().hasReadWrite());
    assertEquals(10, batchDmlRequest.getStatementsCount());
    long index = 0L;
    for (ExecuteBatchDmlRequest.Statement statement : batchDmlRequest.getStatementsList()) {
      assertEquals(insertSql, statement.getSql());
      assertEquals(
          String.valueOf(reverse(initialValue + index)),
          statement.getParams().getFieldsMap().get("p2").getStringValue());
      index++;
    }
  }

  @Test
  public void testHibernatePooledSequenceEntity_skipsExcludedRange() {
    // batch_bit_reversed_generator will skip the range [1,20000] (bit-reversed sequences only
    // support one range that can be skipped, not multiple, so the skipped range is the min/max
    // of all the skipped ranges.

    String getSequenceValuesSql =
        "/* spanner.force_read_write_transaction=true */ "
            + "/* spanner.ignore_during_internal_retry=true */ "
            + " select get_next_sequence_value(sequence pooled_sequence) AS n "
            + "from unnest(generate_array(1, 5))";

    long initialValue = 20000L;
    long expectedId = reverse(initialValue + 1L);
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of(getSequenceValuesSql),
            createBitReversedSequenceResultSet(20001L, 20005L)));

    String insertSql = "insert into `test-entity` (name,id) values (@p1,@p2)";
    mockSpanner.putStatementResult(
        StatementResult.update(
            Statement.newBuilder(insertSql)
                .bind("p1")
                .to((String) null)
                .bind("p2")
                .to(expectedId)
                .build(),
            1L));

    try (SessionFactory sessionFactory =
            createTestHibernateConfig(ImmutableList.of(TestSequenceEntity.class))
                .buildSessionFactory();
        Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      long id = (long) session.save(new TestSequenceEntity());
      assertEquals(expectedId, id);
      transaction.commit();
    }

    // We should have two read/write transactions:
    // 1. A separate transaction for getting the values from the bit-reversed sequence. This
    //    transaction is rolled back.
    // 2. A transaction corresponding to the Hibernate transaction.
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
    assertEquals(1, mockSpanner.countRequestsOfType(RollbackRequest.class));

    assertEquals(
        1,
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
            .filter(request -> request.getSql().equals(getSequenceValuesSql))
            .count());
    ExecuteSqlRequest getSequenceValuesRequest =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
            .filter(request -> request.getSql().equals(getSequenceValuesSql))
            .findFirst()
            .orElseThrow(Suppliers.ofInstance(new AssertionError("missing request")));
    assertTrue(getSequenceValuesRequest.hasTransaction());
    assertTrue(getSequenceValuesRequest.getTransaction().hasBegin());
    assertTrue(getSequenceValuesRequest.getTransaction().getBegin().hasReadWrite());

    assertEquals(
        1,
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
            .filter(request -> request.getSql().equals(insertSql))
            .count());
    ExecuteSqlRequest insertRequest =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
            .filter(request -> request.getSql().equals(insertSql))
            .findFirst()
            .orElseThrow(Suppliers.ofInstance(new AssertionError("missing request")));
    assertTrue(insertRequest.hasTransaction());
    assertTrue(insertRequest.getTransaction().hasBegin());
    assertTrue(insertRequest.getTransaction().getBegin().hasReadWrite());
  }

  @Test
  public void testHibernatePooledSequenceEntity_abortedErrorRetriesSequence() {
    String getSequenceValuesSql =
        "/* spanner.force_read_write_transaction=true */ "
            + "/* spanner.ignore_during_internal_retry=true */ "
            + " select get_next_sequence_value(sequence pooled_sequence) AS n "
            + "from unnest(generate_array(1, 5))";

    long initialValue = 20000L;
    // TODO: Update this once the bug in the mock server has been fixed.
    //       StatementResult.queryAndThen(..) does not work, and always returns the first result.
    long expectedId = reverse(initialValue + 1L);
    mockSpanner.putStatementResult(
        StatementResult.queryAndThen(
            Statement.of(getSequenceValuesSql),
            createBitReversedSequenceResultSet(20001L, 20005L),
            createBitReversedSequenceResultSet(20006L, 20010L)));
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofException(
            mockSpanner.createAbortedException(ByteString.copyFromUtf8("test"))));

    String insertSql = "insert into `test-entity` (name,id) values (@p1,@p2)";
    mockSpanner.putStatementResult(
        StatementResult.update(
            Statement.newBuilder(insertSql)
                .bind("p1")
                .to((String) null)
                .bind("p2")
                .to(expectedId)
                .build(),
            1L));

    try (SessionFactory sessionFactory =
            createTestHibernateConfig(ImmutableList.of(TestSequenceEntity.class))
                .buildSessionFactory();
        Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      long id = (long) session.save(new TestSequenceEntity());
      assertEquals(expectedId, id);
      transaction.commit();
    }

    // We should have three read/write transactions:
    // 1. Two transactions for getting the values from the bit-reversed sequence. The first one is
    //    aborted.
    // 2. A transaction corresponding to the Hibernate transaction.
    // The transactions for getting values from the bit-reversed sequence are rolled back instead
    // of committed, as this prevents the transaction from being aborted on the emulator. Only the
    // transaction that successfully fetched a value from the bit-reversed sequence is rolled back.
    // The aborted transaction is neither committed or rolled back.
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
    assertEquals(1, mockSpanner.countRequestsOfType(RollbackRequest.class));

    // We should have two attempts to get sequence values.
    assertEquals(
        2,
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
            .filter(request -> request.getSql().equals(getSequenceValuesSql))
            .count());
    // We should only have one insert statement.
    assertEquals(
        1,
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
            .filter(request -> request.getSql().equals(insertSql))
            .count());
  }

  @Test
  public void testQueryHint() {
    String expectedSql =
        "select s1_0.id,s1_0.name from Singer @{FORCE_INDEX=idx_singer_active} s1_0";
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of(expectedSql),
            createSingerResultSet(ImmutableList.of(new Singer(1L, "test")))));

    try (SessionFactory sessionFactory =
            createTestHibernateConfig(ENTITY_CLASSES).buildSessionFactory();
        Session session = sessionFactory.openSession()) {
      CriteriaBuilder cb = session.getCriteriaBuilder();
      CriteriaQuery<Singer> cr = cb.createQuery(Singer.class);
      Root<Singer> root = cr.from(Singer.class);
      cr.select(root);
      Query<Singer> query =
          session
              .createQuery(cr)
              .addQueryHint(
                  Hints.forceIndexFrom("Singer", "idx_singer_active", ReplaceMode.ALL).toComment());

      assertNotNull(query.getResultList());
      assertEquals(
          1,
          mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
              .filter(request -> request.getSql().equals(expectedSql))
              .count());
    }
  }

  @Test
  public void testInsertIdentityEntity() {
    String sql = "insert into IdentityEntity (name) values (@p1)\n" + "THEN RETURN *";
    Statement statement = Statement.newBuilder(sql).bind("p1").to("test").build();
    mockSpanner.putStatementResult(
        StatementResult.query(
            statement,
            ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(
                            StructType.newBuilder()
                                .addFields(
                                    Field.newBuilder()
                                        .setName("id")
                                        .setType(Type.newBuilder().setCode(TypeCode.INT64).build())
                                        .build())
                                .addFields(
                                    Field.newBuilder()
                                        .setName("name")
                                        .setType(Type.newBuilder().setCode(TypeCode.STRING).build())
                                        .build())
                                .build())
                        .build())
                .addRows(
                    ListValue.newBuilder()
                        .addValues(
                            Value.newBuilder()
                                .setStringValue(String.valueOf(Math.abs(Long.reverse(1L))))
                                .build())
                        .addValues(Value.newBuilder().setStringValue("test").build())
                        .build())
                .setStats(ResultSetStats.newBuilder().setRowCountExact(1L).build())
                .build()));
    try (SessionFactory sessionFactory =
            createTestHibernateConfig(ImmutableList.of(IdentityEntity.class))
                .buildSessionFactory();
        Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      IdentityEntity entity = new IdentityEntity();
      entity.setName("test");
      session.persist(entity);
      transaction.commit();
      assertEquals(Math.abs(Long.reverse(1L)), entity.getId());
    }
  }

  @Test
  public void testSelectForUpdate() {
    String expectedSql = "select s1_0.id,s1_0.name from Singer s1_0 where s1_0.id=@p1 for update";
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.newBuilder(expectedSql).bind("p1").to(1L).build(),
            createSingerResultSet(ImmutableList.of(new Singer(1L, "test")))));

    try (SessionFactory sessionFactory =
            createTestHibernateConfig(ENTITY_CLASSES).buildSessionFactory();
        Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      Singer singer = session.get(Singer.class, 1L, LockMode.PESSIMISTIC_WRITE);
      assertNotNull(singer);
      assertEquals(
          1,
          mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
              .filter(request -> request.getSql().equals(expectedSql))
              .count());
      transaction.commit();
    }
  }

  @Test
  public void testCollectionWithBatchSize() {
    Singer singer1 = new Singer(1L, "test1");
    Singer singer2 = new Singer(2L, "test2");
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of("select s1_0.id,s1_0.name from Singer s1_0"),
            createSingerResultSet(ImmutableList.of(singer1, singer2))));
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.newBuilder(
                    "select a1_0.singer,a1_0.id from Album a1_0 where a1_0.singer in unnest(@p1)")
                .bind("p1")
                .toInt64Array(new long[] {1L, 2L})
                .build(),
            createAlbumResultSet(
                ImmutableList.of(
                    new Album(singer1, 1L),
                    new Album(singer1, 2L),
                    new Album(singer2, 3L),
                    new Album(singer2, 4L)))));

    try (SessionFactory sessionFactory =
            createTestHibernateConfig(ENTITY_CLASSES).buildSessionFactory();
        Session session = sessionFactory.openSession()) {
      CriteriaBuilder cb = session.getCriteriaBuilder();
      CriteriaQuery<Singer> cr = cb.createQuery(Singer.class);
      Root<Singer> root = cr.from(Singer.class);
      cr.select(root);
      Query<Singer> query = session.createQuery(cr);
      List<Singer> singers = query.getResultList();
      assertNotNull(singers);
      for (Singer singer : singers) {
        List<Album> albums = singer.getAlbums();
        assertEquals(2, albums.size());
      }
    }
  }

  ResultSet createSingerResultSet(List<Singer> singers) {
    return ResultSet.newBuilder()
        .setMetadata(
            ResultSetMetadata.newBuilder()
                .setRowType(
                    StructType.newBuilder()
                        .addFields(
                            Field.newBuilder()
                                .setName("id")
                                .setType(Type.newBuilder().setCode(TypeCode.INT64).build())
                                .build())
                        .addFields(
                            Field.newBuilder()
                                .setName("name")
                                .setType(Type.newBuilder().setCode(TypeCode.STRING).build())
                                .build())
                        .build())
                .build())
        .addAllRows(
            singers.stream()
                .map(
                    singer ->
                        ListValue.newBuilder()
                            .addValues(
                                Value.newBuilder()
                                    .setStringValue(String.valueOf(singer.getId()))
                                    .build())
                            .addValues(Value.newBuilder().setStringValue(singer.getName()).build())
                            .build())
                .collect(Collectors.toList()))
        .build();
  }

  ResultSet createAlbumResultSet(List<Album> albums) {
    return ResultSet.newBuilder()
        .setMetadata(
            ResultSetMetadata.newBuilder()
                .setRowType(
                    StructType.newBuilder()
                        .addFields(
                            Field.newBuilder()
                                .setName("singer")
                                .setType(Type.newBuilder().setCode(TypeCode.INT64).build())
                                .build())
                        .addFields(
                            Field.newBuilder()
                                .setName("id")
                                .setType(Type.newBuilder().setCode(TypeCode.INT64).build())
                                .build())
                        .build())
                .build())
        .addAllRows(
            albums.stream()
                .map(
                    album ->
                        ListValue.newBuilder()
                            .addValues(
                                Value.newBuilder()
                                    .setStringValue(String.valueOf(album.getSinger().getId()))
                                    .build())
                            .addValues(
                                Value.newBuilder()
                                    .setStringValue(String.valueOf(album.getId()))
                                    .build())
                            .build())
                .collect(Collectors.toList()))
        .build();
  }

  @Table(name = "test-entity")
  @Entity
  static class TestSequenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "batch_bit_reversed_generator")
    @GenericGenerator(
        name = "batch_bit_reversed_generator",
        type = PooledBitReversedSequenceStyleGenerator.class,
        parameters = {
          @Parameter(name = "sequence_name", value = "pooled_sequence"),
          @Parameter(name = "increment_size", value = "5"),
          @Parameter(name = "initial_value", value = "500"),
          @Parameter(name = "exclude_ranges", value = "[1,1000] [10000,20000]")
        })
    private long id;

    @Column private String name;
  }

  @Table(name = "test-entity")
  @Entity
  static class NonPooledSequenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test-entity-generator")
    @SequenceGenerator(
        name = "test-entity-generator",
        allocationSize = 1,
        sequenceName = "test-entity-seq")
    private long id;

    @Column private String name;
  }

  static long reverse(long value) {
    long result = Long.reverse(value);
    if (result == Long.MIN_VALUE) {
      return Long.MAX_VALUE;
    }
    if (result < 0L) {
      return Long.reverse(Math.abs(result));
    }
    return result;
  }
}
