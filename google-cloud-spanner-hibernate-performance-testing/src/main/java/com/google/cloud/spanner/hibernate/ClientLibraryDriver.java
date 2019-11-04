package com.google.cloud.spanner.hibernate;

import static com.google.cloud.spanner.hibernate.BenchmarkUtil.benchmark;

public class ClientLibraryDriver {

  private static void setupDatabases(ClientLibraryOperations clientLibraryOperations) {
    clientLibraryOperations.deleteTestDatabase();
    clientLibraryOperations.createTestDatabase();
  }

  public static void main(String[] args) {
    ClientLibraryOperations clientLibraryOperations = new ClientLibraryOperations();

    setupDatabases(clientLibraryOperations);

    benchmark(clientLibraryOperations::createSingleTable, "Create a table.");
    benchmark(
        clientLibraryOperations::singleInsert, "Insert a record into a table.");
    benchmark(
        () -> clientLibraryOperations.batchInsert(1000),
        "Insert 1000 records into a table.");
    benchmark(
        clientLibraryOperations::batchUpdate, "Updates 1000 records in a table.");
    benchmark(
        clientLibraryOperations::deleteSingleTable, "Drops a single table");
    benchmark(clientLibraryOperations::runDdlLarge, "Running bulk DDL operations.");
  }
}
