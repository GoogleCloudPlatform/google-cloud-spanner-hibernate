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
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * Code samples for using Stale Reads in Hibernate.
 *
 * <p>Stale reads allow you to read from the Spanner database at a timestamp in the past. See:
 * https://cloud.google.com/spanner/docs/reads
 */
public class StaleReadsDemo {

  /** Runs the Stale Read demo. */
  public static void main(String[] args) {
    StandardServiceRegistry registry = new StandardServiceRegistryBuilder().configure().build();
    SessionFactory sessionFactory =
        new MetadataSources(registry).buildMetadata().buildSessionFactory();
    SessionHelper sessionHelper = new SessionHelper(sessionFactory);

    runStaleReads(sessionHelper);
  }

  static void runStaleReads(SessionHelper sessionHelper) {
    System.out.println("======== Stale Reads Demo ========");

    Book book;

    try (Session session = sessionHelper.createReadWriteSession()) {
      // First save a book record in the database.
      session.beginTransaction();
      book = new Book("Super Book", "Bob Blob");
      session.save(book);
      session.getTransaction().commit();
      System.out.println("Saved book to database: " + book);

      // Perform a strong read. One book is returned.
      List<Book> booksInTable =
          session
              .createQuery("from Book b where b.id = :id", Book.class)
              .setParameter("id", book.getId())
              .list();
      System.out.println("Executing a strong read: " + booksInTable);
    }

    try (Session session = sessionHelper.createExactStaleReadSession(600)) {
      List<Book> booksInTable =
          session
              .createQuery("from Book b where b.id = :id", Book.class)
              .setParameter("id", book.getId())
              .list();
      System.out.println(
          "Executing a exact stale read 10 minutes in the past (no books should be found): "
              + booksInTable);
    }

    System.out.println("==========================");
  }
}
