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

import com.example.entities.Book;
import com.google.cloud.spanner.jdbc.CloudSpannerJdbcConnection;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Code samples for using Stale Reads in Hibernate.
 *
 * <p>Stale reads allow you to read from the Spanner database at a timestamp in the past.
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
