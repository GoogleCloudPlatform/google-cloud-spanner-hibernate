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

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * An sample Hibernate application demonstrating how to use deeper features of Cloud Spanner.
 *
 * <p>These include:
 * - Different Spanner transaction types
 * - Stale reads
 */
public class SpannerFeatureSampleApplication {

  /**
   * Entry point to the sample application.
   */
  public static void main(String[] args) {
    StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
        .configure()
        .build();
    SessionFactory sessionFactory = new MetadataSources(registry).buildMetadata()
        .buildSessionFactory();

    try {
      // Run the stale reads demo.
      StaleReadsDemo.runStaleReads(sessionFactory);

      // Demos using different Cloud Spanner transaction types
      TransactionTypeDemo.runReadOnlyTransaction(sessionFactory);

    } finally {
      sessionFactory.close();
    }
  }

}
