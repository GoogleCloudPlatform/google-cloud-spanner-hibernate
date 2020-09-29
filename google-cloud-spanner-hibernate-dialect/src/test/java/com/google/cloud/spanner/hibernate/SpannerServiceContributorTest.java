/*
 * Copyright 2019-2020 Google LLC
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.mockrunner.base.NestedApplicationException;
import com.mockrunner.mock.jdbc.MockDriver;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.Test;

/**
 * Tests the {@link SpannerServiceContributor}.
 *
 * @author Mike Eltsufin
 */
public class SpannerServiceContributorTest {

  @Test
  public void testUserAgentContribution() throws SQLException {
    // create mock Driver so we can intercept the call to Driver.connect(url, props)
    AtomicBoolean obtainedConnection = new AtomicBoolean();
    MockDriver mockDriver = new MockDriver() {
      public Connection connect(String url, Properties info) throws SQLException {
        if (info.get("userAgent").equals("sp-hib")) {
          obtainedConnection.set(true);
          return super.connect(url, info);
        }
        throw new SQLException(
            String.format("Missing or unexpected user agent string: %s", info.get("userAgent")));
      }
    };
    mockDriver.setupConnection(mock(Connection.class));

    // make sure our mock driver is discovered by Hibernate
    deregisterDrivers();
    DriverManager.registerDriver(mockDriver);

    StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
        .applySetting("hibernate.dialect", SpannerDialect.class.getName())
        // must NOT set a driver class name so that Hibernate will use java.sql.DriverManager
        // and discover the only mock driver we have set up.
        .applySetting("hibernate.connection.url", "unused")
        .applySetting("hibernate.connection.username", "unused")
        .applySetting("hibernate.connection.password", "unused")
        .build();

    // trigger creation of connections for the connection pool
    Metadata metadata = new MetadataSources(registry).buildMetadata();

    // verify that our mock Driver.connect(url, props) was called and returned a connection.
    assertThat(obtainedConnection.get()).isTrue();
  }

  private void deregisterDrivers() {
    try {
      Enumeration<Driver> drivers = DriverManager.getDrivers();
      while (drivers.hasMoreElements()) {
        DriverManager.deregisterDriver(drivers.nextElement());
      }
    } catch (SQLException exc) {
      throw new NestedApplicationException(exc);
    }
  }

}

