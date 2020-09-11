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

package com.google.cloud.spanner.hibernate;

import com.google.cloud.spanner.hibernate.entities.Airport;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.query.Query;

/**
 * Runs common operations (reads, writes) on Spanner using Hibernate.
 */
public class HibernateOperations {

  private StandardServiceRegistry registry;
  private SessionFactory sessionFactory;

  /**
   * Constructs {@link HibernateOperations} object.
   */
  public HibernateOperations() {
    this.registry = new StandardServiceRegistryBuilder().configure().build();
  }

  /**
   * Creates Spanner tables for Hibernate entities. Tables are created as a side-effect of
   * building the session factory.
   */
  public void initializeEntityTables() {
    this.sessionFactory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
  }

  /**
   * Inserts a number of rows into Spanner using Hibernate.
   * @param rowCount number of rows to insert.
   */
  public void insertRows(int rowCount) {
    runTransaction(session -> {
      for (int i = 0; i < rowCount; i++) {
        Airport airport = new Airport(
            "The Airport",
            "100 main street",
            "United States",
            new Date(),
            (long) (1000 * Math.random()));

        session.save(airport);
      }
    });
  }

  /**
   * Inserts a number of rows into Spanner using Hibernate.
   * @param rowCount number of rows to update.
   */
  public void updateRows(int rowCount) {
    runTransaction(session -> {
      CriteriaBuilder builder = session.getCriteriaBuilder();

      CriteriaQuery<Airport> criteria = builder.createQuery(Airport.class);
      Root<Airport> rootEntry = criteria.from(Airport.class);
      criteria.select(rootEntry);

      Query<Airport> airportQuery = session.createQuery(criteria);
      List<Airport> airportList =
          airportQuery.stream().limit(rowCount).collect(Collectors.toList());

      for (Airport airport : airportList) {
        airport.setCountry("Canada");
        airport.setPlaneCapacity((long) (Math.random() * 10000));
        session.update(airport);
      }
    });
  }

  private void runTransaction(Consumer<Session> hibernateOperations) {
    Session session = this.sessionFactory.openSession();
    Transaction tx = session.beginTransaction();
    hibernateOperations.accept(session);
    tx.commit();
    session.close();
  }
}
