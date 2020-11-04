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
import javax.persistence.PersistenceException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Code samples specifying a different Cloud Spanner Transaction Type.
 */
public class TransactionTypeDemo {

  /**
   * This code sample demonstrates an error when trying to perform an update in a read-only
   * transaction.
   */
  static void runReadOnlyTransaction(SessionFactory sessionFactory) {
    System.out.println("======== Read-only Transaction Demo ========");

    try (Session session = sessionFactory.openSession()) {
      session.beginTransaction();

      // Set transaction to read-only.
      session.doWork(conn -> {
        conn.createStatement().execute("SET TRANSACTION READ ONLY");
      });

      Book book = new Book("Programming Guide", "Author");
      session.save(book);

      session.getTransaction().commit();
    } catch (PersistenceException ex) {
      System.out.println("You will get the following error if you try to modify the tables in "
          + "a read-only transaction: ");

      Throwable throwable = ex;
      while (throwable != null) {
        System.out.println("\t" + throwable.getMessage());
        throwable = throwable.getCause();
      }
    }

    System.out.println("==========================");
  }
}
