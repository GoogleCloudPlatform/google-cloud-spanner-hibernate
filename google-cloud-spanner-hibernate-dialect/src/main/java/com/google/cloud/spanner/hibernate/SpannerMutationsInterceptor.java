/*
 * Copyright 2019-2022 Google LLC
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

import com.google.cloud.spanner.jdbc.CloudSpannerJdbcConnection;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.hibernate.engine.transaction.internal.TransactionImpl;
import org.hibernate.internal.AbstractSharedSessionContract;
import org.hibernate.internal.SessionImpl;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;

/**
 * Interceptor that can be used in combination with the Cloud Spanner Hibernate dialect to instruct
 * the Cloud Spanner JDBC driver to use mutations instead of DML. This can increase the performance
 * of the application.
 */
public class SpannerMutationsInterceptor extends EmptyInterceptor {
  private AbstractSharedSessionContract session;
  private final Method flushBeforeTransactionCompletion;
  private boolean changedMutationsConversion = false;

  /** Public constructor is necessary for instantiation by reflection. */
  public SpannerMutationsInterceptor() {
    Method method;
    try {
      method = JdbcSessionOwner.class.getDeclaredMethod("flushBeforeTransactionCompletion");
    } catch (NoSuchMethodException e) {
      method = null;
    }
    flushBeforeTransactionCompletion = method;
  }

  @Override
  public void afterTransactionBegin(Transaction tx) {
    if (tx instanceof TransactionImpl) {
      TransactionImpl transaction = (TransactionImpl) tx;
      try {
        Field sessionField = TransactionImpl.class.getDeclaredField("session");
        sessionField.setAccessible(true);
        session = (AbstractSharedSessionContract) sessionField.get(transaction);
      } catch (Throwable ignore) {
        // Do nothing and continue.
      }
    }
  }

  @Override
  public void preFlush(Iterator entities) {
    // Switch to mutations if the flush is triggered by a commit, as we then know that these changes
    // will not be read by this transaction after the flush.
    if (session != null && isFlushTriggeredByCommit()) {
      session.doWork(
          connection -> {
            CloudSpannerJdbcConnection cloudSpannerJdbcConnection =
                connection.unwrap(CloudSpannerJdbcConnection.class);
            if (cloudSpannerJdbcConnection != null
                && !cloudSpannerJdbcConnection.isConvertDmlToMutations()) {
              changedMutationsConversion = true;
              cloudSpannerJdbcConnection.setConvertDmlToMutations(true);
            }
          });
    }
  }

  @Override
  public void afterTransactionCompletion(Transaction tx) {
    if (changedMutationsConversion && session != null) {
      session.doWork(
          connection -> {
            CloudSpannerJdbcConnection cloudSpannerJdbcConnection =
                connection.unwrap(CloudSpannerJdbcConnection.class);
            if (cloudSpannerJdbcConnection != null) {
              cloudSpannerJdbcConnection.setConvertDmlToMutations(false);
              // Reset the flag here, as there could in theory be multiple flushes for one session.
              changedMutationsConversion = false;
            }
          });
    }
  }

  boolean isFlushTriggeredByCommit() {
    // Unfortunately there is no good way to get information on why a flush was triggered, other
    // than looking at the callstack. We know that all flushes that are triggered by a commit go
    // through the method xyz.
    if (flushBeforeTransactionCompletion == null) {
      return false;
    }
    StackTraceElement[] stackElements = Thread.currentThread().getStackTrace();
    for (StackTraceElement stack : stackElements) {
      if (SessionImpl.class.getName().equals(stack.getClassName())
          && stack.getMethodName().equals(flushBeforeTransactionCompletion.getName())) {
        return true;
      }
    }
    return false;
  }
}
