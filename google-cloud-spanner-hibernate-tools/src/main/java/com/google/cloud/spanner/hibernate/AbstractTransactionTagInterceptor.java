/*
 * Copyright 2019-2024 Google LLC
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

package com.google.cloud.spanner.hibernate;

import java.lang.reflect.Field;
import javax.annotation.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.transaction.internal.TransactionImpl;
import org.hibernate.internal.SessionFactoryImpl;

/** Base class for interceptors that add transaction tags. */
public abstract class AbstractTransactionTagInterceptor implements Interceptor {
  private final Field sessionField;

  private Boolean dialectIsPostgres;

  protected AbstractTransactionTagInterceptor() {
    try {
      sessionField = TransactionImpl.class.getDeclaredField("session");
      sessionField.setAccessible(true);
    } catch (NoSuchFieldException noSuchFieldException) {
      throw new HibernateException("Could not get 'session' field of TransactionImpl");
    }
  }

  @Override
  public void afterTransactionBegin(Transaction tx) {
    String tag = getTag();
    if (tag != null) {
      Session session = getSession(tx);
      if (session != null) {
        session.doWork(connection -> {
          if (!(connection.isReadOnly() || connection.getAutoCommit())) {
            connection.createStatement().execute(generateSetTransactionTagStatement(session, tag));
          }
        });
      }
    }
  }

  private String generateSetTransactionTagStatement(Session session, String tag) {
    if (dialectIsPostgres(session)) {
      return "set spanner.transaction_tag='" + tag + "'";
    }
    return "set transaction_tag='" + tag + "'";
  }

  private boolean dialectIsPostgres(Session session) {
    if (this.dialectIsPostgres == null) {
      synchronized (this) {
        if (this.dialectIsPostgres == null) {
          SessionFactory factory = session.getSessionFactory();
          if (factory instanceof SessionFactoryImpl) {
            Dialect dialect = ((SessionFactoryImpl) factory).getJdbcServices().getDialect();
            this.dialectIsPostgres = dialect.openQuote() == '"';
          } else {
            this.dialectIsPostgres = false;
          }
        }
      }
    }
    return this.dialectIsPostgres;
  }

  /** Returns the tag that should be added to the transaction that is being started. */
  protected abstract String getTag();

  /**
   * Gets the session from the transaction.
   * Unfortunately, there is no public API to do so, so we have to use reflection.
   */
  private @Nullable Session getSession(Transaction tx) {
    if (tx instanceof TransactionImpl && sessionField != null) {
      try {
        TransactionImpl transaction = (TransactionImpl) tx;
        return (Session) sessionField.get(transaction);
      } catch (IllegalAccessException illegalAccessException) {
        throw new HibernateException(
            "Failed to get session from transaction", illegalAccessException);
      }
    }
    return null;
  }

}
