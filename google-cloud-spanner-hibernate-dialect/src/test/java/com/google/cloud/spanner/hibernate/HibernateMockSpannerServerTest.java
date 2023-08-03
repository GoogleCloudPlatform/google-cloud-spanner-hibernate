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

import static org.junit.Assert.assertEquals;

import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.hibernate.entities.Singer;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.Type;
import com.google.spanner.v1.TypeCode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Test;

/** Tests Hibernate configuration using an in-memory mock Spanner server. */
public class HibernateMockSpannerServerTest extends AbstractMockSpannerServerTest {
  private static final ImmutableList<Class<?>> ENTITY_CLASSES = ImmutableList.of(Singer.class);

  @Test
  public void testHibernateGetSinger() {
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.newBuilder(
                    "select singer0_.id as id1_0_0_ from Singer singer0_ where singer0_.id=@p1")
                .bind("p1")
                .to(1L)
                .build(),
            com.google.spanner.v1.ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(
                            StructType.newBuilder()
                                .addFields(
                                    Field.newBuilder()
                                        .setName("id1_0_0_")
                                        .setType(Type.newBuilder().setCode(TypeCode.INT64).build())
                                        .build())
                                .build())
                        .build())
                .addRows(
                    ListValue.newBuilder()
                        .addValues(Value.newBuilder().setStringValue("1").build())
                        .build())
                .build()));

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
            Statement.of("select next_val as id_val from singerId"),
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
            Statement.of("select next_val as id_val from singerId"),
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
}
