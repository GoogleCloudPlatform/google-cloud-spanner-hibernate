/*
 * Copyright 2019-2024 Google LLC
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

import com.google.cloud.spanner.connection.SpannerPool;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.boot.SpringApplication;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/** Runs the sample application on the emulator. */
@RunWith(JUnit4.class)
public class SampleApplicationEmulatorTest {
  private static GenericContainer<?> emulator;

  /** Starts the emulator in a test container. */
  @BeforeClass
  public static void setup() {
    emulator =
        new GenericContainer<>(
                DockerImageName.parse("gcr.io/cloud-spanner-emulator/emulator:latest"))
            .withExposedPorts(9010)
            .waitingFor(Wait.forListeningPort());
    emulator.start();
  }

  /** Stops the emulator. */
  @AfterClass
  public static void cleanup() {
    SpannerPool.closeSpannerPool();
    if (emulator != null) {
      emulator.stop();
    }
  }

  @Test
  public void testRunApplication() {
    System.setProperty("spanner.emulator", "true");
    System.setProperty("spanner.host", "//localhost:" + emulator.getMappedPort(9010));
    SpringApplication.run(SampleApplication.class).close();
  }
}
