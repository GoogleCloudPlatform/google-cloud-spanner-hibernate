package org.hibernate.dialect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.dialect.entities.ListEntity;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

public class SpannerColumnTypeTest extends BaseEntityManagerFunctionalTestCase {

  // Lists of data to read and write to Spanner array columns.
  private static final List<BigDecimal> BIG_DECIMAL_LIST =
      Arrays.asList(new BigDecimal(10000L));
  private static final List<Boolean> BOOLEAN_LIST = Arrays.asList(true, false);
  private static final List<byte[]> BYTES_LIST =
      Arrays.asList("hello".getBytes(), "good bye".getBytes());
  private static final List<Date> DATE_LIST =
      Arrays.asList(new Date(1000), new Date(2000));
  private static final List<Double> DOUBLE_LIST = Arrays.asList(1.0, 2.0);
  private static final List<Integer> INT_LIST = Arrays.asList(1, 2, 3);
  private static final List<String> STRING_LIST = Arrays.asList("hello", "good bye");

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class<?>[]{
      ListEntity.class
    };
  }

  @Test
  public void testArrayColumnMappings() {
    super.buildEntityManagerFactory();

    doInJPA(this::entityManagerFactory, entityManager -> {
      ListEntity listEntity = new ListEntity();

      listEntity.setBigDecimalList(BIG_DECIMAL_LIST);
      listEntity.setBooleanList(BOOLEAN_LIST);
      listEntity.setByteList(BYTES_LIST);
      listEntity.setDateList(DATE_LIST);
      listEntity.setDoubleList(DOUBLE_LIST);
      listEntity.setIntList(INT_LIST);
      listEntity.setStringList(STRING_LIST);

      entityManager.persist(listEntity);
      entityManager.flush();

      Session session = entityManager.unwrap(Session.class);
      List<ListEntity> listEntities =
          session.createQuery(
              "from ListEntity", ListEntity.class).list();
      assertThat(listEntities).hasSize(1);

      ListEntity result = listEntities.get(0);

      assertThat(result.getBigDecimalList()).isEqualTo(BIG_DECIMAL_LIST);
      assertThat(result.getBooleanList()).isEqualTo(BOOLEAN_LIST);
      assertThat(result.getByteList()).isEqualTo(BYTES_LIST);
      assertThat(result.getDateList()).isEqualTo(DATE_LIST);
      assertThat(result.getDoubleList()).isEqualTo(DOUBLE_LIST);
      assertThat(result.getIntList()).isEqualTo(INT_LIST);
      assertThat(result.getStringList()).isEqualTo(STRING_LIST);
    });
  }
}
