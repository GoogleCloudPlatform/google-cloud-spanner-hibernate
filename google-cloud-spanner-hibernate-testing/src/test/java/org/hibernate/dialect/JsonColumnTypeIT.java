package org.hibernate.dialect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.dialect.entities.JsonEntity;
import org.hibernate.dialect.entities.JsonEntity.Address;
import org.hibernate.dialect.entities.JsonEntity.Employee;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

public class JsonColumnTypeIT extends BaseEntityManagerFunctionalTestCase {

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class<?>[]{
        JsonEntity.class
    };
  }

  @Test
  public void jsonColumnTest() {
    doInJPA(this::entityManagerFactory, entityManager -> {
      JsonEntity jsonEntity = new JsonEntity();
      jsonEntity.setEmployee(createEmployee());

      entityManager.persist(jsonEntity);
      entityManager.flush();

      Session session = entityManager.unwrap(Session.class);
      List<JsonEntity> entities =
          session.createQuery(
              "from JsonEntity", JsonEntity.class).list();
      assertThat(entities).hasSizeGreaterThan(0);

      JsonEntity entity = entities.get(0);
      assertThat(entity.getEmployee()).isEqualTo(createEmployee());
    });
  }

  private static Employee createEmployee() {
    Employee employee = new Employee();
    employee.setName("Helen");
    employee.setAddress(new Address("123 Main Street", 12345));
    return employee;
  }
}
