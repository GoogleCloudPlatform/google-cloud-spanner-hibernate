/*
 * Copyright 2019-2020 Google LLC
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
import static org.junit.Assert.assertEquals;

import com.example.entities.Album;
import com.example.entities.Payment;
import com.example.entities.Person;
import com.example.entities.Singer;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This verifies the sample application.
 *
 * @author Chengyuan Zhao
 * @author Daniel Zou
 */
public class SampleApplicationIT {

  private final StandardServiceRegistry registry =
      new StandardServiceRegistryBuilder().configure().build();

  private final SessionFactory sessionFactory =
      new MetadataSources(registry).buildMetadata().buildSessionFactory();

  private Session session;

  @Before
  public void setupTest() {
    session = sessionFactory.openSession();
  }

  @After
  public void cleanup() {
    session.close();
  }

  @Test
  public void testSavePerson() {
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
            .map(payment -> payment.getAmount().longValue())
            .collect(Collectors.toList());
    assertThat(paymentAmounts).containsExactlyInAnyOrder(200L, 600L);
  }

  @Test
  public void testSaveSingers() {
    SampleApplication.saveSingerAlbum(session);

    List<Singer> savedSingers = session.createQuery("from Singer", Singer.class).list();

    assertThat(savedSingers).hasSize(1);

    Singer singer = savedSingers.get(0);
    assertThat(singer.getName()).isEqualTo("Singer1");
    assertThat(singer.getAlbums()).hasSize(1);
    assertEquals(ImmutableList.of("nick_name1", "nickname_2"), singer.getNickNames());

    Album album = singer.getAlbums().get(0);
    assertThat(album.getTitle()).isEqualTo("Album name");
    assertThat(album.getSinger()).isEqualTo(singer);
  }
}
