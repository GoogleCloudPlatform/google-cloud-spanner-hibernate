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

import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * A wrapper class for the {@link org.hibernate.SessionFactory} class to help access different read
 * modes and transactions types for Cloud Spanner.
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

  /** Creates a read-write session. */
  public Session createReadWriteSession() {
    Session session = sessionFactory.openSession();
    session.doWork(
        conn -> {
          // read-write transactions always use strong reads.
          conn.setReadOnly(false);
        });
    return session;
  }

  /** Creates a read-only session. */
  public Session createReadOnlySession() {
    Session session = sessionFactory.openSession();
    session.doWork(
        conn -> {
          conn.setReadOnly(true);
          conn.createStatement().execute("SET READ_ONLY_STALENESS = 'STRONG'");
        });
    return session;
  }

  /** Create a session for exact stale reads at {@code stalenessSeconds} in the past. */
  public Session createExactStaleReadSession(int stalenessSeconds) {
    Session session = sessionFactory.openSession();
    session.doWork(
        conn -> {
          conn.setReadOnly(true);
          conn.createStatement()
              .execute("SET READ_ONLY_STALENESS = 'EXACT_STALENESS " + stalenessSeconds + "s'");
        });
    return session;
  }
}
