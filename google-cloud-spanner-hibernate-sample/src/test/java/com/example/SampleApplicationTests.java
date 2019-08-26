/*
 * Copyright 2019 Google LLC
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

package com.example;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.Test;

/**
 * This verifies the sample application.
 *
 * @author Chengyuan Zhao
 * @author Daniel Zou
 */
public class SampleApplicationTests {

  private final StandardServiceRegistry registry =
      new StandardServiceRegistryBuilder()
          .configure()
          .build();

  private final SessionFactory sessionFactory =
      new MetadataSources(registry)
          .buildMetadata()
          .buildSessionFactory();

  @Test
  public void testSample() {
    Session session = sessionFactory.openSession();
    SampleApplication.savePerson(session);

    List<Person> savedPersons = session.createQuery("from Person", Person.class).list();

    assertThat(savedPersons).hasSize(1);

    Person person = savedPersons.get(0);
    assertThat(person.getName()).isEqualTo("person");
    assertThat(person.getNickName()).isEqualTo("purson");
    assertThat(person.getAddress()).isEqualTo("address");

    List<Payment> payments = person.getPayments();
    assertThat(payments).hasSize(2);

    List<Long> paymentAmounts =
        payments.stream()
            .map(Payment::getAmount)
            .collect(Collectors.toList());
    assertThat(paymentAmounts).containsExactlyInAnyOrder(200L, 600L);
  }
}
