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

import static com.google.cloud.spanner.hibernate.BenchmarkUtil.benchmark;

import org.apache.log4j.Logger;

/**
 * Runs Spanner Client Library operations and benchmarks their performance.
 */
public class ClientLibraryDriver {

  private static final Logger LOGGER = Logger.getLogger(ClientLibraryDriver.class);

  /**
   * Runs and benchmarks Spanner Client Library operations.
   */
  public static void main(String[] args) {
    ClientLibraryOperations clientLibraryOperations = new ClientLibraryOperations();

    LOGGER.info("Resetting the Spanner database to run the performance tests.");
    clientLibraryOperations.resetTestDatabase();

    benchmark(clientLibraryOperations::createSingleTable, "Create a table.");

    benchmark(
        clientLibraryOperations::singleInsert,
        "Insert a record into a table with single-use transaction.");

    benchmark(
        () -> clientLibraryOperations.batchInsert(1),
        "Insert 1 record into a table with a batch size == 1.");

    benchmark(
        () -> clientLibraryOperations.batchInsert(1000),
        "Insert 1000 records into a table in a batch.");

    benchmark(
        () -> clientLibraryOperations.batchUpdate(1),
        "Updates 1 record in a table in a batch.");

    benchmark(
        () -> clientLibraryOperations.batchUpdate(1000),
        "Updates 1000 records in a table in a batch.");

    // benchmark(
    //     clientLibraryOperations::deleteSingleTable, "Drops a single table");

    benchmark(clientLibraryOperations::runDdlLarge, "Running bulk DDL operations.");
  }
}
