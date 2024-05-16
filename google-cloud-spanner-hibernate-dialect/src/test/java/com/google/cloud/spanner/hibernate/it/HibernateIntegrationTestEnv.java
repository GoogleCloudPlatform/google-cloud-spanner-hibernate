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

package com.google.cloud.spanner.hibernate.it;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.InstanceConfigId;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.InstanceNotFoundException;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.SpannerOptions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.cfg.Configuration;

/** Test environment used for integration tests. */
public class HibernateIntegrationTestEnv {

  // Spanner host URL should be set through this system property. The default is the default Spanner
  // host URL.
  public static final String TEST_SPANNER_URL_PROPERTY = "HIBERNATE_SPANNER_HOST";
  // ProjectId should be set through this system property.
  public static final String TEST_PROJECT_PROPERTY = "HIBERNATE_TEST_PROJECT";
  // InstanceId should be set through this system property.
  public static final String TEST_INSTANCE_PROPERTY = "HIBERNATE_TEST_INSTANCE";
  // DatabaseId should be set through this system property.
  public static final String TEST_DATABASE_PROPERTY = "HIBERNATE_TEST_DATABASE";
  private static final Logger logger =
      Logger.getLogger(HibernateIntegrationTestEnv.class.getName());
  // Default fallback project Id will be used if one isn't set via the system property.
  private static final String DEFAULT_PROJECT_ID = "span-cloud-testing";
  // Default instance id.
  private static final String DEFAULT_INSTANCE_ID = "hibernate-testing";
  // Default database id.
  private static final String DEFAULT_DATABASE_ID = "test-db";
  // Shared Spanner instance that is automatically created and closed.
  private final Spanner spanner;
  // Spanner URL.
  private final String spannerHost;
  private final String projectId;
  private final String instanceId;
  private final String databaseId;
  private final String hostUrl;
  private Database database;

  /** Constructs an integration test environment for Hibernate. */
  public HibernateIntegrationTestEnv() {
    projectId = System.getProperty(TEST_PROJECT_PROPERTY, DEFAULT_PROJECT_ID);
    instanceId = System.getProperty(TEST_INSTANCE_PROPERTY, DEFAULT_INSTANCE_ID);
    String databaseIdFormat = System.getProperty(TEST_DATABASE_PROPERTY, DEFAULT_DATABASE_ID);
    databaseId = generateDatabaseId(databaseIdFormat);
    hostUrl = System.getProperty(TEST_SPANNER_URL_PROPERTY);

    spannerHost = getSpannerUrl();
    logger.info("Using Spanner host: " + spannerHost);
    SpannerOptions options = createSpannerOptions();
    spanner = options.getService();
  }

  public Spanner getSpanner() {
    return spanner;
  }

  public String getSpannerUrl() {
    return hostUrl;
  }

  public String getProjectId() {
    return projectId;
  }

  public String getInstanceId() {
    return instanceId;
  }

  private String generateDatabaseId(String databaseIdFormat) {
    String id =
        String.format("%s_%s", databaseIdFormat, UUID.randomUUID().toString().replace('-', '_'));
    // Make sure the database id is not longer than the max allowed 30 characters.
    if (id.length() > 30) {
      id = id.substring(0, 30);
    }
    // Database ids may not end with a hyphen or an underscore.
    if (id.endsWith("-") || id.endsWith("_")) {
      id = id.substring(0, id.length() - 1);
    }
    return id;
  }

  /** Creates a test database for this test environment. This method may only be called once. */
  public Database createDatabase(Iterable<String> ddlStatements) {
    if (database != null) {
      throw new IllegalStateException("The test database has already been created.");
    }
    Spanner spanner = getSpanner();
    InstanceAdminClient instanceAdminClient = spanner.getInstanceAdminClient();
    try {
      instanceAdminClient.getInstance(instanceId);
    } catch (InstanceNotFoundException notFoundException) {
      try {
        InstanceConfigId instanceConfigId =
            instanceAdminClient.listInstanceConfigs().iterateAll().iterator().next().getId();
        instanceAdminClient
            .createInstance(
                instanceAdminClient
                    .newInstanceBuilder(InstanceId.of(projectId, instanceId))
                    .setInstanceConfigId(instanceConfigId)
                    .setDisplayName("Hibernate test instance")
                    .setNodeCount(1)
                    .build())
            .get();
      } catch (ExecutionException executionException) {
        SpannerException spannerException =
            SpannerExceptionFactory.asSpannerException(executionException.getCause());
        // Ignore if it ALREADY_EXISTS. This is caused by multiple test runs trying simultaneously
        // to create an instance.
        if (spannerException.getErrorCode() != ErrorCode.ALREADY_EXISTS) {
          throw spannerException;
        }
      } catch (InterruptedException interruptedException) {
        throw SpannerExceptionFactory.propagateInterrupt(interruptedException);
      }
    }

    DatabaseAdminClient client = spanner.getDatabaseAdminClient();
    OperationFuture<Database, CreateDatabaseMetadata> op =
        client.createDatabase(
            client.newDatabaseBuilder(DatabaseId.of(projectId, instanceId, databaseId)).build(),
            ddlStatements);
    try {
      database = op.get();
      logger.log(Level.INFO, "Created database [" + database.getId() + "]");
      return database;
    } catch (ExecutionException e) {
      throw SpannerExceptionFactory.asSpannerException(e.getCause());
    } catch (InterruptedException e) {
      throw SpannerExceptionFactory.propagateInterrupt(e);
    }
  }

  /** Updates the schema of the test database. */
  public void updateDdl(String databaseId, Iterable<String> statements)
      throws ExecutionException, InterruptedException {
    Spanner spanner = getSpanner();
    DatabaseAdminClient client = spanner.getDatabaseAdminClient();
    OperationFuture<Void, UpdateDatabaseDdlMetadata> op =
        client.updateDatabaseDdl(instanceId, databaseId, statements, null);
    op.get();
    logger.log(Level.INFO, "DDL was updated by {0}.", String.join(" and ", statements));
  }

  /**
   * Writes data to the given test database.
   *
   * @param mutations The mutations to write
   */
  public void write(Iterable<Mutation> mutations) {
    Spanner spanner = getSpanner();
    DatabaseId db = DatabaseId.of(projectId, instanceId, databaseId);
    DatabaseClient dbClient = spanner.getDatabaseClient(db);
    dbClient.write(mutations);
  }

  private SpannerOptions createSpannerOptions() {
    SpannerOptions.Builder builder = SpannerOptions.newBuilder().setProjectId(projectId);
    if (spannerHost != null) {
      builder.setHost(spannerHost);
    }
    return builder.build();
  }

  /** Drops all the databases that were created by this test env. */
  public void cleanup() {
    if (database != null) {
      try {
        database.drop();
      } catch (Exception e) {
        logger.log(Level.WARNING, "Failed to drop test database " + database.getId(), e);
      }
    }
    if (spanner != null) {
      spanner.close();
    }
  }

  String createTestJdbcUrl() {
    String url =
        String.format(
            "jdbc:cloudspanner:/projects/%s/instances/%s/databases/%s",
            projectId, instanceId, databaseId);
    if (!Strings.isNullOrEmpty(System.getenv("SPANNER_EMULATOR_HOST"))) {
      url += ";autoConfigEmulator=true";
    }
    return url;
  }

  Configuration createTestHibernateConfig(Iterable<Class<?>> entityClasses) {
    return createTestHibernateConfig(entityClasses, ImmutableMap.of());
  }

  protected Configuration createTestHibernateConfig(
      Iterable<Class<?>> entityClasses, Map<String, String> hibernateProperties) {
    Configuration config = new Configuration();

    config.setProperty(
        "hibernate.connection.driver_class", "com.google.cloud.spanner.jdbc.JdbcDriver");
    config.setProperty("hibernate.connection.url", createTestJdbcUrl());
    config.setProperty("hibernate.dialect", "com.google.cloud.spanner.hibernate.SpannerDialect");
    for (Entry<String, String> property : hibernateProperties.entrySet()) {
      config.setProperty(property.getKey(), property.getValue());
    }
    for (Class<?> entityClass : entityClasses) {
      config.addAnnotatedClass(entityClass);
    }

    return config;
  }
}
