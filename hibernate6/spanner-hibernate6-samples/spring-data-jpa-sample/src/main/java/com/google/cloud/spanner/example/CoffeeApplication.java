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

package com.google.cloud.spanner.example;

import com.google.cloud.spanner.connection.SpannerPool;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

/** Sample application using spring-boot-data-jpa with Cloud Spanner and Hibernate 6.x. */
@SpringBootApplication
@EntityScan(basePackages = {"com.google.cloud.spanner.example.model"})
@EnableJpaRepositories(basePackages = {"com.google.cloud.spanner.example.model"})
public class CoffeeApplication {

  /**
   * Helper class for starting the Cloud Spanner emulator on a fixed port. It is recommended to use
   * a dynamically assigned port for most test setups.
   */
  static class SpannerEmulatorFixedPortContainer
      extends FixedHostPortGenericContainer<SpannerEmulatorFixedPortContainer> {
    public SpannerEmulatorFixedPortContainer(@NotNull String dockerImageName) {
      super(dockerImageName);

      addFixedExposedPort(9010, 9010);
      addFixedExposedPort(9020, 9020);
      setWaitStrategy(
          new LogMessageWaitStrategy().withRegEx(".*Cloud Spanner emulator running\\..*"));
    }
  }

  private static final Logger log = LoggerFactory.getLogger(CoffeeApplication.class);

  static final SpannerEmulatorFixedPortContainer EMULATOR =
      new SpannerEmulatorFixedPortContainer("gcr.io/cloud-spanner-emulator/emulator");

  /** Startup method for the sample application. */
  public static void main(String[] args) {
    // Start the Cloud Spanner emulator in an embedded Docker container.
    EMULATOR.start();
    SpringApplication.run(CoffeeApplication.class, args);
    SpringApplication.getShutdownHandlers()
        .add(
            () -> {
              log.info("Closing Spanner pool");
              SpannerPool.closeSpannerPool();
              log.info("Stopping Cloud Spanner emulator");
              EMULATOR.stop();
              EMULATOR.close();
            });
  }
}
