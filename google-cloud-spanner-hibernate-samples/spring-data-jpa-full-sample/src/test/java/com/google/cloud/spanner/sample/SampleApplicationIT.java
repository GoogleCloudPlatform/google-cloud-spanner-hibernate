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

import static org.junit.Assert.assertTrue;

import com.google.api.gax.rpc.ResourceExhaustedException;
import com.google.cloud.spanner.IntegrationTest;
import com.google.cloud.spanner.SpannerOptionsHelper;
import com.google.cloud.trace.v1.TraceServiceClient;
import com.google.cloud.trace.v1.TraceServiceClient.ListTracesPagedResponse;
import com.google.cloud.trace.v1.TraceServiceSettings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.devtools.cloudtrace.v1.ListTracesRequest;
import io.opentelemetry.api.GlobalOpenTelemetry;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Category(IntegrationTest.class)
@RunWith(JUnit4.class)
public class SampleApplicationIT {

  private static final HibernateIntegrationTestEnv TEST_ENV = new HibernateIntegrationTestEnv();

  @BeforeClass
  public static void createDatabase() {
    TEST_ENV.createDatabase(ImmutableList.of());
  }

  @AfterClass
  public static void dropTestDatabase() {
    TEST_ENV.cleanup();
  }

  @Test
  public void testRunApplication() throws Exception {
    SpannerOptionsHelper.resetActiveTracingFramework();
    GlobalOpenTelemetry.resetForTest();

    String serviceName = "spanner-spring-data-jpa-sample-" + ThreadLocalRandom.current().nextInt();
    System.setProperty("open_telemetry.service_name", serviceName);
    System.setProperty("spanner.project", TEST_ENV.getProjectId());
    System.setProperty("spanner.instance", TEST_ENV.getInstanceId());
    System.setProperty("spanner.database", TEST_ENV.getDatabaseId());
    SampleApplication.main(new String[] {});

    assertTrace(serviceName);
  }

  private void assertTrace(String serviceName) throws Exception {
    TraceServiceSettings settings = TraceServiceSettings.newBuilder().build();
    try (TraceServiceClient client = TraceServiceClient.create(settings)) {
      // It can take a few seconds before the trace is visible.
      Thread.sleep(5000L);
      boolean foundTrace = false;
      for (int attempts = 0; attempts < 20; attempts++) {
        ListTracesPagedResponse response =
            client.listTraces(
                ListTracesRequest.newBuilder()
                    .setProjectId(TEST_ENV.getProjectId())
                    .setFilter("service.name:\"" + serviceName + "\"")
                    .build());
        int size = Iterables.size(response.iterateAll());
        if (size > 0) {
          foundTrace = true;
          break;
        } else {
          Thread.sleep(5000L);
        }
      }
      assertTrue("No traces found", foundTrace);
    } catch (ResourceExhaustedException resourceExhaustedException) {
      if (resourceExhaustedException
          .getMessage()
          .contains("Quota exceeded for quota metric 'Read requests (free)'")) {
        // Ignore and allow the test to succeed.
        System.out.println("RESOURCE_EXHAUSTED error ignored");
      } else {
        throw resourceExhaustedException;
      }
    }
  }
}
