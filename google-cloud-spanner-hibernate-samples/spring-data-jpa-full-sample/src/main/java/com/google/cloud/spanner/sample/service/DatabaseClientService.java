/*
 * Copyright 2019-2025 Google LLC
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

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.jdbc.CloudSpannerJdbcConnection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

/**
 * Sample service for accessing the underlying Spanner {@link
 * com.google.cloud.spanner.DatabaseClient} to use specific Spanner features.
 */
@Service
public class DatabaseClientService {

  @PersistenceContext private EntityManager entityManager;

  public DatabaseClientService() {}

  /**
   * Returns the underlying {@link DatabaseClient} that is used by the JDBC connection. Note that a
   * {@link DatabaseClient} is stateless and that one {@link DatabaseClient} is shared across
   * multiple JDBC connections. It is perfectly safe to store a reference to the {@link
   * DatabaseClient} that is returned by this method and use it at any moment. Transactions that are
   * executed using this {@link DatabaseClient} does not affect any of the JDBC connections that are
   * created by JPA / Hibernate.
   */
  public DatabaseClient getSpannerClient() {
    // Unwrap the Spanner JDBC driver and return the underlying DatabaseClient.
    // This DatabaseClient can safely be used in parallel with JPA / Hibernate.
    return entityManager
        .unwrap(Session.class)
        .doReturningWork(
            connection -> connection.unwrap(CloudSpannerJdbcConnection.class).getDatabaseClient());
  }
}
