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

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.Date;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Runs common operations (reads, writes) on Spanner using the Spanner Client Libraries.
 */
public class ClientLibraryOperations {
  private static final String DDL_SMALL = "/ddl_strings/airport_ddl.txt";
  private static final String DDL_LARGE = "/ddl_strings/bulk_ddl.txt";

  private static final String PROJECT_ID = "cloud-spanner-hibernate-ci";
  private static final String INSTANCE_NAME = "test-instance";
  private static final String DATABASE_NAME = "hibernate-performance-testing";

  private static final String AIRPORT_TABLE = "airport";
  private static final String AIRPORT_INSERT_TEMPLATE =
      "INSERT INTO airport "
          + "(id, address, country, date_built, name, plane_capacity) "
          + "VALUES (@id, @address, @country, @date_built, @name, @plane_capacity)";

  private final Spanner spanner;
  private final DatabaseAdminClient databaseAdminClient;
  private final DatabaseClient databaseClient;

  /**
   * Constructs the {@link ClientLibraryOperations} object.
   */
  public ClientLibraryOperations() {
    SpannerOptions spannerOptions = SpannerOptions.newBuilder().setProjectId(PROJECT_ID).build();
    this.spanner = spannerOptions.getService();

    this.databaseAdminClient = this.spanner.getDatabaseAdminClient();
    this.databaseClient = this.spanner.getDatabaseClient(
          DatabaseId.of(PROJECT_ID, INSTANCE_NAME, DATABASE_NAME));
  }

  /**
   * Resets the test Spanner database.
   */
  public void resetTestDatabase() {
    databaseAdminClient.dropDatabase(INSTANCE_NAME, DATABASE_NAME);
    try {
      databaseAdminClient.createDatabase(
          INSTANCE_NAME, DATABASE_NAME, Collections.EMPTY_LIST).get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Runs the DDL statement to create a single table.
   */
  public void createSingleTable() {
    List<String> airportDdl = loadDdlStrings(DDL_SMALL);
    OperationFuture<Void, UpdateDatabaseDdlMetadata> ddlFuture =
        this.databaseAdminClient.updateDatabaseDdl(
            INSTANCE_NAME, DATABASE_NAME, airportDdl, null);

    try {
      ddlFuture.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Drops a single table.
   */
  public void deleteSingleTable() {
    OperationFuture<Void, UpdateDatabaseDdlMetadata> ddlFuture =
        this.databaseAdminClient.updateDatabaseDdl(
            INSTANCE_NAME,
            DATABASE_NAME,
            Collections.singletonList("DROP TABLE " + AIRPORT_TABLE), null);

    try {
      ddlFuture.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Runs DDL to create a large number of tables.
   */
  public void runDdlLarge() {
    List<String> airportDdl = loadDdlStrings(DDL_LARGE);
    OperationFuture<Void, UpdateDatabaseDdlMetadata> ddlFuture =
        this.databaseAdminClient.updateDatabaseDdl(
            INSTANCE_NAME, DATABASE_NAME, airportDdl, null);

    try {
      ddlFuture.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Inserts a row into a table.
   */
  public void singleInsert() {
    this.databaseClient.singleUse().executeQuery(buildSingleInsert());
  }

  /**
   * Executes a batch of inserts to a table.
   */
  public void batchInsert(int batchSize) {
    ArrayList<Mutation> mutations = new ArrayList<>();
    for (int i = 0; i < batchSize; i++) {
      mutations.add(buildSingleMutationInsert());
    }
    this.databaseClient.write(mutations);
  }

  /**
   * Updates batch of existing records to different values.
   */
  public void batchUpdate() {
    Statement statement = Statement.newBuilder("SELECT * FROM " + AIRPORT_TABLE).build();
    ResultSet resultSet =
        databaseClient.singleUseReadOnlyTransaction().executeQuery(statement);

    ArrayList<String> allRowIds = new ArrayList<>();
    while (resultSet.next()) {
      allRowIds.add(resultSet.getCurrentRowAsStruct().getString("id"));
    }

    // Updates all airport's plane_capacity field.
    ArrayList<Mutation> updateMutations = new ArrayList<>();
    for (String id : allRowIds) {
      Mutation mutation = Mutation.newUpdateBuilder(AIRPORT_TABLE)
          .set("id").to(id)
          .set("plane_capacity").to("2222")
          .build();

      updateMutations.add(mutation);
    }

    this.databaseClient.write(updateMutations);
  }

  private static Mutation buildSingleMutationInsert() {
    Mutation mutation = Mutation.newInsertBuilder(AIRPORT_TABLE)
        .set("id").to(UUID.randomUUID().toString())
        .set("address").to("100 Main Street")
        .set("country").to("United States")
        .set("date_built").to(Date.parseDate("2000-04-14"))
        .set("name").to("Foobar Airport")
        .set("plane_capacity").to((int) (1000 * Math.random()))
        .build();
    return mutation;
  }

  private static Statement buildSingleInsert() {
    Statement statement =
        Statement.newBuilder(AIRPORT_INSERT_TEMPLATE)
            .bind("id").to(UUID.randomUUID().toString())
            .bind("address").to("100 Main Street")
            .bind("country").to("United States")
            .bind("date_built").to(Date.parseDate("2000-04-14"))
            .bind("name").to("Foobar Airport")
            .bind("plane_capacity").to((int) (1000 * Math.random()))
            .build();

    return statement;
  }

  private static List<String> loadDdlStrings(String filePath) {
    InputStream resourceStream = ClientLibraryOperations.class.getResourceAsStream(filePath);
    List<String> ddlStrings = new BufferedReader(new InputStreamReader(resourceStream))
        .lines()
        .collect(Collectors.toList());
    return ddlStrings;
  }
}
