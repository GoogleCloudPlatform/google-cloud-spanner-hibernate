/*
 * Copyright 2019-2023 Google LLC
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

package com.google.cloud.spanner.sample.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service class for executing stale reads. */
@Service
public class StaleReadService {

  @PersistenceContext private EntityManager entityManager;

  /** Returns the current timestamp from Cloud Spanner. */
  public OffsetDateTime getCurrentTimestamp() {
    return entityManager
        .unwrap(Session.class)
        .doReturningWork(
            connection -> {
              try (ResultSet resultSet =
                  connection.createStatement().executeQuery("select current_timestamp")) {
                if (resultSet.next()) {
                  return resultSet.getObject(1, OffsetDateTime.class);
                }
                // This should not happen.
                throw new IllegalStateException("No timestamp returned by the server");
              }
            });
  }

  /** Executes a read-only transaction at the given exact timestamp. */
  @Transactional(readOnly = true)
  public <T> T executeReadOnlyTransactionAtTimestamp(
      OffsetDateTime timestamp, Supplier<T> transaction) {
    return executeReadOnlyTransactionWithStaleness(
        "read_timestamp " + timestamp.format(DateTimeFormatter.ISO_DATE_TIME), transaction);
  }

  /** Executes a read-only transaction with the given staleness. */
  @Transactional(readOnly = true)
  public <T> T executeReadOnlyTransactionWithStaleness(String staleness, Supplier<T> transaction) {
    return entityManager
        .unwrap(Session.class)
        .doReturningWork(
            connection -> {
              try (Statement statement = connection.createStatement()) {
                try {
                  statement.execute(String.format("set read_only_staleness='%s'", staleness));
                  return transaction.get();
                } catch (Throwable t) {
                  statement.execute("rollback");
                  throw t;
                } finally {
                  // NOTE: Calling 'commit' if there is no active transaction is a no-op. That means
                  // that if the transaction was rolled back in case of an exception, this commit
                  // will be a no-op.
                  statement.execute("commit");
                  // Reset the read_only_staleness of the connection.
                  connection.createStatement().execute("set read_only_staleness='strong'");
                }
              }
            });
  }
}
