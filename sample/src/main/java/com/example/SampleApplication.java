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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * An example Hibernate application using the Cloud Spanner Dialect.
 *
 * @author Chengyuan Zhao
 */
public class SampleApplication {

  public static void main(String[] args) {

    final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
        .configure()
        .build();
    SessionFactory sessionFactory = new MetadataSources(registry).buildMetadata()
        .buildSessionFactory();
    Session session = sessionFactory.openSession();

    session.beginTransaction();

    Person person = new Person();

    person.setName("person");
    person.setNickName("purson");
    person.setAddress("address");

    session.save(person);

    session.getTransaction().commit();

    session.beginTransaction();

    System.out.println(
        "Found saved Person with generated ID: " + session.createQuery("from Person").list()
            .get(0));

  }
}
