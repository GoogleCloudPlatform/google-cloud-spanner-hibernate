package com.example;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * A wrapper class for the {@link org.hibernate.SessionFactory} class to help access different
 * read modes and transactions types for Cloud Spanner.
 *
 * <p>NOTE: Because JDBC connections are pooled and re-used by sessions, if you alter the JDBC
 * connection (such as setting it to read-only), newly created sessions may unexpectedly inherit
 * settings made by previous sessions. Therefore, it helps to have a wrapper class to manage
 * creating new sessions, such as this.
 */
public class SessionHelper {

  private final SessionFactory sessionFactory;

  public SessionHelper(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  public Session createReadWriteSession() {
    Session session = sessionFactory.openSession();
    session.doWork(conn -> {
      conn.setReadOnly(false);
      conn.createStatement().execute("SET READ_ONLY_STALENESS = 'STRONG'");
    });
    return session;
  }

  public Session createReadOnlySession() {
    Session session = sessionFactory.openSession();
    session.doWork(conn -> {
      conn.setReadOnly(true);
      conn.createStatement().execute("SET READ_ONLY_STALENESS = 'STRONG'");
    });
    return session;
  }

  public Session createExactStaleReadSession(int stalenessSeconds) {
    Session session = sessionFactory.openSession();
    session.doWork(conn -> {
      conn.setReadOnly(true);
      conn.createStatement().execute(
          "SET READ_ONLY_STALENESS = 'EXACT_STALENESS " + stalenessSeconds + "s'");
    });
    return session;
  }
}
