/*
 * Copyright 2023 Google LLC
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

package org.hibernate.dialect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.dialect.entities.ListEntity;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

public class SpannerColumnTypeTest extends BaseEntityManagerFunctionalTestCase {

  // Lists of data to read and write to Spanner array columns.
  private static final List<BigDecimal> BIG_DECIMAL_LIST = Arrays.asList(new BigDecimal(10000L));
  private static final List<Boolean> BOOLEAN_LIST = Arrays.asList(true, false);
  private static final List<byte[]> BYTES_LIST =
      Arrays.asList("hello".getBytes(), "good bye".getBytes());
  private static final List<Timestamp> TIMESTAMP_LIST =
      Arrays.asList(new Timestamp(1000), new Timestamp(2000));
  private static final List<Double> DOUBLE_LIST = Arrays.asList(1.0, 2.0);
  private static final List<Integer> INT_LIST = Arrays.asList(1, 2, 3);
  private static final List<String> STRING_LIST = Arrays.asList("hello", "good bye");

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class<?>[] {ListEntity.class};
  }

  @Test
  public void testArrayColumnMappings() {
    super.buildEntityManagerFactory();

    doInJPA(
        this::entityManagerFactory,
        entityManager -> {
          ListEntity listEntity = new ListEntity();

          listEntity.setBigDecimalList(BIG_DECIMAL_LIST);
          listEntity.setBooleanList(BOOLEAN_LIST);
          listEntity.setByteList(BYTES_LIST);
          listEntity.setTimestampList(TIMESTAMP_LIST);
          listEntity.setDoubleList(DOUBLE_LIST);
          listEntity.setIntList(INT_LIST);
          listEntity.setStringList(STRING_LIST);

          entityManager.persist(listEntity);
          entityManager.flush();

          Session session = entityManager.unwrap(Session.class);
          List<ListEntity> listEntities =
              session.createQuery("from ListEntity", ListEntity.class).list();
          assertThat(listEntities).hasSize(1);

          ListEntity result = listEntities.get(0);

          assertThat(result.getBigDecimalList()).isEqualTo(BIG_DECIMAL_LIST);
          assertThat(result.getBooleanList()).isEqualTo(BOOLEAN_LIST);
          assertThat(result.getByteList()).isEqualTo(BYTES_LIST);
          assertThat(result.getTimestampList()).isEqualTo(TIMESTAMP_LIST);
          assertThat(result.getDoubleList()).isEqualTo(DOUBLE_LIST);
          assertThat(result.getIntList()).isEqualTo(INT_LIST);
          assertThat(result.getStringList()).isEqualTo(STRING_LIST);
        });
  }
}
