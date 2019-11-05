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

package com.google.cloud.spanner.hibernate;

import com.google.cloud.spanner.hibernate.entities.Airport;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.FlushModeType;
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

final class HibernateOperations {

  private StandardServiceRegistry registry;
  private SessionFactory sessionFactory;

  HibernateOperations() {
    this.registry = new StandardServiceRegistryBuilder().configure().build();
  }

  void initializeEntityTables() {
    this.sessionFactory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
  }

  void insertRows(int rowCount) {
    Session session = this.sessionFactory.openSession();
    session.setFlushMode(FlushModeType.COMMIT);

    Transaction tx = session.beginTransaction();

    for (int i = 0; i < rowCount; i++) {
      Airport airport = new Airport(
          "The Airport",
          i + " main street",
          "United States",
          new Date(),
          (long) (1000 * Math.random()));

      session.save(airport);
    }

    tx.commit();
    session.close();
  }

  void updateRows(int rowCount) {
    Session session = this.sessionFactory.openSession();
    session.setFlushMode(FlushModeType.COMMIT);
    Transaction tx = session.beginTransaction();

    CriteriaBuilder builder = session.getCriteriaBuilder();

    CriteriaQuery<Airport> criteria = builder.createQuery(Airport.class);
    Root<Airport> rootEntry = criteria.from(Airport.class);
    criteria.select(rootEntry);

    Query<Airport> airportQuery = session.createQuery(criteria);
    List<Airport> airportList = airportQuery.stream().limit(rowCount).collect(Collectors.toList());

    for (Airport airport : airportList) {
      airport.setCountry("Canada");
      airport.setPlaneCapacity((long) (Math.random() * 10000));
      session.update(airport);
    }

    tx.commit();
    session.close();
  }
}
