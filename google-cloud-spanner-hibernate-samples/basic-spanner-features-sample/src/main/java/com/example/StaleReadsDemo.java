package com.example;

import com.example.entities.Book;
import com.google.cloud.spanner.jdbc.CloudSpannerJdbcConnection;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Code samples for using Stale Reads in Hibernate.
 *
 * Stale reads allow you to read from the Spanner database at a timestamp in the past.
 * See: https://cloud.google.com/spanner/docs/reads
 */
public class StaleReadsDemo {

  static void runStaleReads(SessionFactory sessionFactory) {
    System.out.println("======== Stale Reads Demo ========");

    Book book;

    try (Session session = sessionFactory.openSession()) {
      // First save a book record in the database.
      session.beginTransaction();
      book = new Book("Super Book", "Bob Blob");
      session.save(book);
      session.getTransaction().commit();
      System.out.println("Saved book to database: " + book);

      // Perform a strong read. One book is returned.
      List<Book> booksInTable =
          session.createQuery("from Book b where b.id = :id", Book.class)
              .setParameter("id", book.getId())
              .list();
      System.out.println("Executing a strong read: " + booksInTable);
    }

    try (Session session = sessionFactory.openSession()) {
      // Configure the connection to do a stale read.
      session.doWork(conn -> {
        CloudSpannerJdbcConnection jdbcConnection = (CloudSpannerJdbcConnection) conn;
        // Must set to read-only connection to use stale reads.
        jdbcConnection.setReadOnly(true);
        // Set the stale read settings through the JDBC connection.
        jdbcConnection.createStatement().execute(
            "SET READ_ONLY_STALENESS = 'EXACT_STALENESS 600s'");
      });

      List<Book> booksInTable =
          session.createQuery("from Book b where b.id = :id", Book.class)
              .setParameter("id", book.getId())
              .list();
      System.out.println(
          "Executing a exact stale read 10 minutes in the past (no books should be found): "
              + booksInTable);

      // Reset the connection.
      session.doWork(conn -> {
        CloudSpannerJdbcConnection jdbcConnection = (CloudSpannerJdbcConnection) conn;
        jdbcConnection.setReadOnly(false);
        jdbcConnection.createStatement().execute(
            "SET READ_ONLY_STALENESS = 'STRONG'");
      });
    }

    System.out.println("==========================");
  }
}
