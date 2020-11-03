package com.example;

import com.example.entities.Book;
import javax.persistence.PersistenceException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Code samples specifying a different Cloud Spanner Transaction Type.
 */
public class TransactionTypeDemo {

  static void runReadOnlyTransaction(SessionFactory sessionFactory) {
    System.out.println("======== Read-only Transaction Demo ========");

    try (Session session = sessionFactory.openSession()) {
      session.beginTransaction();

      session.doWork(connection -> connection.setReadOnly(true));

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
