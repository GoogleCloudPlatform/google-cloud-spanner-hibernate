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

import java.util.List;
import org.hibernate.Session;
import org.hibernate.dialect.entities.JsonEntity;
import org.hibernate.dialect.entities.JsonEntity.Address;
import org.hibernate.dialect.entities.JsonEntity.Employee;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

public class JsonColumnTypeTest extends BaseEntityManagerFunctionalTestCase {

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class<?>[] {JsonEntity.class};
  }

  @Test
  public void jsonColumnTest() {
    doInJPA(
        this::entityManagerFactory,
        entityManager -> {
          JsonEntity jsonEntity = new JsonEntity();
          jsonEntity.setEmployee(createEmployee());

          entityManager.persist(jsonEntity);
          entityManager.flush();

          Session session = entityManager.unwrap(Session.class);
          List<JsonEntity> entities =
              session.createQuery("from JsonEntity", JsonEntity.class).list();
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
