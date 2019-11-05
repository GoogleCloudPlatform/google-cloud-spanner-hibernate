/*
 * Copyright 2019 Google LLC
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

import org.apache.log4j.Logger;

/**
 * Runs and benchmarks common operations on Spanner using Hibernate.
 */
public class HibernateDriver {

  private static final Logger LOGGER = Logger.getLogger(HibernateDriver.class);

  /**
   * Runs and benchmarks Hibernate operations.
   */
  public static void main(String[] args) {
    LOGGER.info("Resetting the test database.");
    ClientLibraryOperations clientLibraryOperations = new ClientLibraryOperations();
    clientLibraryOperations.resetTestDatabase();

    HibernateOperations hibernateOperations = new HibernateOperations();

    BenchmarkUtil.benchmark(
        hibernateOperations::initializeEntityTables,
        "Create 1 Spanner table from Hibernate entity classes.");

    BenchmarkUtil.benchmark(
        () -> hibernateOperations.insertRows(1),
        "Insert a single row through persisting a Hibernate entity.");

    BenchmarkUtil.benchmark(
        () -> hibernateOperations.insertRows(1000),
        "Insert 1000 rows by saving Hibernate entities.");

    BenchmarkUtil.benchmark(
        () -> hibernateOperations.updateRows(1),
        "Update 1 row by saving Hibernate entities.");

    BenchmarkUtil.benchmark(
        () -> hibernateOperations.updateRows(1000),
        "Update 1000 row by saving Hibernate entities.");
  }
}
