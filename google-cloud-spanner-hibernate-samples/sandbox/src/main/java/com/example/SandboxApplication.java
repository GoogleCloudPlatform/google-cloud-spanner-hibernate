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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
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
public class SandboxApplication {

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

    session.beginTransaction();

    Singer singer = new Singer();
    singer.setNumbers(Arrays.asList(1, 2, 3));
    session.save(singer);

    session.getTransaction().commit();

    List<Singer> singersInTable =
        session.createQuery("from Singer", Singer.class).list();
    System.out.println(singersInTable);

    session.close();
    sessionFactory.close();
  }
}
