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

import com.example.entities.Album;
import com.example.entities.CreditCardPayment;
import com.example.entities.Person;
import com.example.entities.Singer;
import com.example.entities.WireTransferPayment;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * An example Hibernate application using the Google Cloud Spanner Dialect for Hibernate ORM.
 *
 * @author Chengyuan Zhao
 * @author Daniel Zou
 */
public class SampleApplication {

  /**
   * Main method that runs a simple console application that saves a {@link Person} entity and then
   * retrieves it to print to the console.
   */
  public static void main(String[] args) {

    // Create Hibernate environment objects.
    StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
        .configure()
        .build();
    SessionFactory sessionFactory = new MetadataSources(registry).buildMetadata()
        .buildSessionFactory();
    Session session = sessionFactory.openSession();

    // Save an entity into Spanner Table.
    savePerson(session);

    // Save a singer entity into the Spanner Table.
    saveSingerAlbum(session);
  }

  /**
   * Saves a {@link Person} entity into a Spanner table.
   */
  public static void savePerson(Session session) {
    session.beginTransaction();

    WireTransferPayment payment1 = new WireTransferPayment();
    payment1.setWireId("1234ab");
    payment1.setAmount(200L);

    CreditCardPayment payment2 = new CreditCardPayment();
    payment2.setCreditCardId("creditcardId");
    payment2.setAmount(600L);

    Person person = new Person();
    person.setName("person");
    person.setNickName("purson");
    person.setAddress("address");

    person.addPayment(payment1);
    person.addPayment(payment2);

    session.save(person);
    session.getTransaction().commit();

    List<Person> personsInTable =
        session.createQuery("from Person", Person.class).list();

    System.out.println(
        String.format("There are %d persons saved in the table:", personsInTable.size()));

    for (Person personInTable : personsInTable) {
      System.out.println(personInTable);
    }
  }

  /**
   * Saves a {@link Singer} entity into a Spanner table.
   *
   * <p>Demonstrates saving entities using {@link com.google.cloud.spanner.hibernate.Interleaved}.
   */
  public static void saveSingerAlbum(Session session) {
    session.beginTransaction();

    Singer singer = new Singer("Singer1", new ArrayList<>());
    Album album = new Album(singer, "Album name");
    singer.addAlbum(album);

    session.save(singer);
    session.save(album);
    session.getTransaction().commit();

    List<Singer> singers =
        session.createQuery("from Singer", Singer.class).list();
    System.out.println(
        String.format("There are %d singer saved in the table:", singers.size()));

    for (Singer singerInTable : singers) {
      System.out.println(singerInTable);
    }
  }
}
