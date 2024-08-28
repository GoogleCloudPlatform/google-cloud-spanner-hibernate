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

package com.google.cloud.spanner.sample.opentelemetry;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.opentelemetry.trace.TraceConfiguration;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import com.google.cloud.spanner.SpannerOptions;
import com.google.common.base.Strings;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * This class initializes {@link io.opentelemetry.api.OpenTelemetry} before Spring initializes a
 * DataSource. It is important that {@link io.opentelemetry.api.OpenTelemetry} is created before a
 * data source is created, as it allows the Spanner JDBC driver to pick up the OpenTelemetry
 * configuration and use this for all JDBC connections.
 */
public class OpenTelemetryInitializer
    implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

  @Override
  public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
    ConfigurableEnvironment environment = event.getEnvironment();
    boolean enabled =
        Boolean.TRUE.equals(environment.getProperty("open_telemetry.enabled", Boolean.class));
    String project = environment.getProperty("open_telemetry.project");
    String serviceName = environment.getProperty("open_telemetry.service_name");
    if (Strings.isNullOrEmpty(serviceName)) {
      serviceName = "spanner-spring-data-jpa-sample-" + ThreadLocalRandom.current().nextInt();
    }

    if (!enabled || Strings.isNullOrEmpty(project)) {
      return;
    }

    // Enable OpenTelemetry tracing in Spanner.
    SpannerOptions.enableOpenTelemetryTraces();

    if (!hasDefaultCredentials()) {
      // Do not create an OpenTelemetry object if this environment does not have any default
      // credentials configured. This could for example be on local test environments that use
      // the Spanner emulator. This will trigger the use of OpenTelemetry.noop().
      return;
    }

    TraceConfiguration traceConfiguration =
        TraceConfiguration.builder().setProjectId(project).build();
    SpanExporter traceExporter = TraceExporter.createWithConfiguration(traceConfiguration);

    // Create an OpenTelemetry object and register it as the global OpenTelemetry object. This
    // will automatically be picked up by the Spanner libraries and used for tracing.
    OpenTelemetrySdk.builder()
        .setTracerProvider(
            SdkTracerProvider.builder()
                // Set sampling to 'AlwaysOn' in this example. In production, you want to reduce
                // this to a smaller fraction to limit the number of traces that are being
                // collected.
                .setSampler(Sampler.alwaysOn())
                .setResource(Resource.builder().put("service.name", serviceName).build())
                .addSpanProcessor(BatchSpanProcessor.builder(traceExporter).build())
                .build())
        .buildAndRegisterGlobal();
  }

  private boolean hasDefaultCredentials() {
    try {
      return GoogleCredentials.getApplicationDefault() != null;
    } catch (IOException exception) {
      return false;
    }
  }
}
