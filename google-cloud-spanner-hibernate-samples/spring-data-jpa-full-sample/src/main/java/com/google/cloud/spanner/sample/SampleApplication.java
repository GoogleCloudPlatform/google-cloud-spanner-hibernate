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

package com.google.cloud.spanner.sample;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.connection.SpannerPool;
import com.google.cloud.spanner.sample.entities.Concert;
import com.google.cloud.spanner.sample.entities.Singer;
import com.google.cloud.spanner.sample.opentelemetry.OpenTelemetryInitializer;
import com.google.cloud.spanner.sample.repository.ConcertRepository;
import com.google.cloud.spanner.sample.service.BatchService;
import com.google.cloud.spanner.sample.service.ConcertService;
import com.google.cloud.spanner.sample.service.DatabaseClientService;
import com.google.cloud.spanner.sample.service.SingerService;
import com.google.cloud.spanner.sample.service.StaleReadService;
import jakarta.annotation.PreDestroy;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sample application using Spring Boot Data JPA (Hibernate) with PGAdapter and a Cloud Spanner
 * database.
 *
 * <p>This sample shows how to do the following:
 *
 * <ol>
 *   <li>Use auto-generated sequential primary key values without the risk of creating hotspots
 *   <li>Use interleaved tables with Spring Boot Data JPA (Hibernate)
 *   <li>How to map all supported data types to the corresponding Java types
 *   <li>How to execute read/write and read-only transactions
 *   <li>How to execute stale reads on Cloud Spanner
 *   <li>How to use OpenTelemetry with Spanner and Hibernate
 * </ol>
 */
@SpringBootApplication
public class SampleApplication implements CommandLineRunner {
  private static final Logger log = LoggerFactory.getLogger(SampleApplication.class);

  private final SingerService singerService;
  private final ConcertService concertService;
  /**
   * The {@link StaleReadService} is a generic service that can be used to execute workloads using
   * stale reads. Stale reads can perform better than strong reads. See <a
   * href="https://cloud.google.com/spanner/docs/timestamp-bounds">https://cloud.google.com/spanner/docs/timestamp-bounds</a>
   * for more information.
   */
  private final StaleReadService staleReadService;

  private final BatchService batchService;

  private final ConcertRepository concertRepository;

  private final DatabaseClientService databaseClientService;

  /** Constructor with auto-injected dependencies. */
  public SampleApplication(
      SingerService singerService,
      ConcertService concertService,
      StaleReadService staleReadService,
      BatchService batchService,
      ConcertRepository concertRepository,
      DatabaseClientService databaseClientService) {
    this.singerService = singerService;
    this.concertService = concertService;
    this.staleReadService = staleReadService;
    this.batchService = batchService;
    this.concertRepository = concertRepository;
    this.databaseClientService = databaseClientService;
  }

  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(SampleApplication.class);
    // Add an application listener that initializes OpenTelemetry BEFORE any data source is created
    // by Spring. This ensures that the Spanner JDBC driver can pick up the OpenTelemetry
    // configuration and use this for all JDBC connections that are created.
    OpenTelemetryInitializer openTelemetryInitializer = new OpenTelemetryInitializer();
    application.addListeners(openTelemetryInitializer);
    application.run(args).close();

    SpannerPool.closeSpannerPool();
    if (openTelemetryInitializer.getOpenTelemetrySdk() != null) {
      openTelemetryInitializer.getOpenTelemetrySdk().close();
    }
  }

  @Override
  public void run(String... args) {
    // First clear the current tables in one batch.
    batchService.deleteAllData();

    // Generate some random data in one batch.
    batchService.generateRandomData();

    // Print some of the randomly inserted data.
    printData();
    // Show how to do a stale read.
    staleRead();

    // Select all active singers. This query uses a FORCE_INDEX query hint.
    List<Singer> activeSingers = singerService.getActiveSingers();
    log.info("Found {} active singers", activeSingers.size());

    // Write 10 random singers to the database using mutations.
    writeMutations();
  }

  void printData() {
    Random random = new Random();
    // Fetch and print some data using a read-only transaction.
    for (int n = 0; n < 3; n++) {
      char c = (char) (random.nextInt(26) + 'a');
      singerService.printSingersWithLastNameStartingWith(String.valueOf(c).toUpperCase());
    }
  }

  void staleRead() {
    // Check the number of concerts at this moment in the database.
    log.info("Found {} concerts using a strong read", concertRepository.findAll().size());
    // Insert a new concert and then do a stale read. That concert should then not be included in
    // the result of the stale read.
    OffsetDateTime currentTime = staleReadService.getCurrentTimestamp();
    log.info("Inserting a new concert");
    concertService.generateRandomConcerts(1);
    // List all concerts using a stale read. The read timestamp is before the insert of the latest
    // concert, which means that it will not be included in the query result, and the number of
    // concerts returned should be the same as the first query in this method.
    List<Concert> concerts =
        staleReadService.executeReadOnlyTransactionAtTimestamp(
            currentTime, concertRepository::findAll);
    log.info("Found {} concerts using a stale read.", concerts.size());
  }

  /**
   * This sample methods gets a reference to the underlying Spanner {@link
   * com.google.cloud.spanner.DatabaseClient} and uses that to write mutations to Spanner. The
   * {@link com.google.cloud.spanner.DatabaseClient} that is returned is safe to use in parallel
   * with JPA / Hibernate, and it is safe to store a reference to the client for later use.
   */
  void writeMutations() {
    DatabaseClient client = databaseClientService.getSpannerClient();
    // Generate 10 random singers and write these to the database using mutations.
    singerService.insertSingersUsingMutations(client, 10);
  }

  /**
   * This method is automatically called when the application is shut down, and closes the
   * underlying session pools that were used by the Cloud Spanner client libraries.
   */
  @PreDestroy
  public void onExit() {
    try {
      // Clean up the internal Spanner session pool when we are exiting the application.
      SpannerPool.closeSpannerPool();
    } catch (SpannerException ignore) {
      // Ignore any errors and continue the shutdown.
    }
  }
}
