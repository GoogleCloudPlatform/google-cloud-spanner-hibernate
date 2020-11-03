package com.example;

import com.google.spanner.v1.TransactionOptions;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * An sample Hibernate application demonstrating how to use deeper features of Cloud Spanner.
 *
 * These include:
 * - Different Spanner transaction types
 * - Stale reads
 */
public class SpannerFeatureSampleApplication {

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
