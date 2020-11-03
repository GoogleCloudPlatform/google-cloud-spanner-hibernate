package com.example;

import com.example.entities.Book;
import com.google.cloud.spanner.jdbc.CloudSpannerJdbcConnection;
import java.util.List;
import org.hibernate.Session;
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
 * - Native queries
 * - Partitioned DML
 */
public class SpannerFeatureSampleApplication {

  public static void main(String[] args) {
    StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
        .configure()
        .build();
    SessionFactory sessionFactory = new MetadataSources(registry).buildMetadata()
        .buildSessionFactory();
    Session session = sessionFactory.openSession();

    runStaleRead(session);

    session.close();
    sessionFactory.close();
  }

  /**
   * Stale reads allow you to read from the Spanner database at a timestamp in the past.
   * See: https://cloud.google.com/spanner/docs/reads
   */
  private static void runStaleRead(Session session) {
    // First save a book record in the database.
    session.beginTransaction();
    Book book = new Book("Super Book", "Bob Blob");
    session.save(book);
    session.getTransaction().commit();

    List<Book> booksInTable =
        session.createQuery("from Book", Book.class).list();
    System.out.println("Executing a strong read: " + booksInTable + " " + booksInTable.size());

    session.doWork(conn -> {
      CloudSpannerJdbcConnection jdbcConnection = (CloudSpannerJdbcConnection) conn;
      jdbcConnection.createStatement().execute("SET READ_ONLY_STALENESS = 'EXACT_STALENESS 1200s'");
      jdbcConnection.commit();
    });

    booksInTable = session.createQuery("from Book", Book.class).list();
    System.out.println("Executing a stale read (20 minutes in the past): " + booksInTable + " " + booksInTable.size());
  }

}
