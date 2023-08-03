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

package com.google.cloud.spanner.hibernate;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.MockSpannerServiceImpl;
import com.google.cloud.spanner.admin.database.v1.MockDatabaseAdminImpl;
import com.google.cloud.spanner.connection.SpannerPool;
import com.google.common.collect.ImmutableMap;
import com.google.longrunning.Operation;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import com.google.spanner.v1.SpannerGrpc;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Map.Entry;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/** Abstract base class for Hibernate tests using an in-mem mock Spanner server. */
public abstract class AbstractMockSpannerServerTest {
  protected static MockSpannerServiceImpl mockSpanner;
  protected static MockDatabaseAdminImpl mockDatabaseAdmin;
  private static Server server;

  /** Setup in-memory mock Spanner server. */
  @BeforeClass
  public static void setup() throws IOException {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    mockDatabaseAdmin = new MockDatabaseAdminImpl();
    InetSocketAddress address = new InetSocketAddress("localhost", 0);
    server =
        NettyServerBuilder.forAddress(address)
            .addService(mockSpanner)
            .addService(mockDatabaseAdmin)
            .intercept(
                new ServerInterceptor() {
                  @Override
                  public <ReqT, RespT> Listener<ReqT> interceptCall(
                      ServerCall<ReqT, RespT> serverCall,
                      Metadata metadata,
                      ServerCallHandler<ReqT, RespT> serverCallHandler) {

                    if (SpannerGrpc.getExecuteStreamingSqlMethod()
                        .getFullMethodName()
                        .equals(serverCall.getMethodDescriptor().getFullMethodName())) {
                      String userAgent =
                          metadata.get(
                              Metadata.Key.of(
                                  "x-goog-api-client", Metadata.ASCII_STRING_MARSHALLER));
                      assertNotNull(userAgent);
                      assertTrue(userAgent.contains("sp-hib"));
                    }
                    return Contexts.interceptCall(
                        Context.current(), serverCall, metadata, serverCallHandler);
                  }
                })
            .build()
            .start();
  }

  /** Stop and cleanup mock Spanner server. */
  @AfterClass
  public static void teardown() throws InterruptedException {
    SpannerPool.closeSpannerPool();
    server.shutdown();
    server.awaitTermination();
  }

  @After
  public void clearRequests() {
    mockSpanner.clearRequests();
    mockDatabaseAdmin.reset();
  }

  protected void addDdlResponseToSpannerAdmin() {
    mockDatabaseAdmin.addResponse(
        Operation.newBuilder()
            .setDone(true)
            .setResponse(Any.pack(Empty.getDefaultInstance()))
            .setMetadata(Any.pack(UpdateDatabaseDdlMetadata.getDefaultInstance()))
            .build());
  }

  protected void addDdlExceptionToSpannerAdmin() {
    mockDatabaseAdmin.addException(
        Status.INVALID_ARGUMENT.withDescription("Statement is invalid.").asRuntimeException());
  }

  protected String createTestJdbcUrl() {
    return String.format(
        "jdbc:cloudspanner://localhost:%d/projects/my-project/instances/my-instance"
            + "/databases/my-database?usePlainText=true",
        server.getPort());
  }

  protected Configuration createTestHibernateConfig(Iterable<Class<?>> entityClasses) {
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
