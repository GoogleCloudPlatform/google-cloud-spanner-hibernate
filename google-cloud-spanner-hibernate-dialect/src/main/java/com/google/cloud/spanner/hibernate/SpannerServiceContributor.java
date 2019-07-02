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

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceContributor;

/**
 * An implementation of a Hibernate {@link ServiceContributor} which provides a "userAgent" JDBC
 * connection property to the Spanner JDBC driver to identify the library.
 *
 * <p>Note that Hibernate will automatically pass down all "hibernate.connection.*" properties
 * without the prefix to {@code Driver.connect(url, props)}.
 *
 * @author Mike Eltsufin
 */
public class SpannerServiceContributor implements ServiceContributor {

  static final String HIBERNATE_API_CLIENT_LIB_TOKEN = "sp-hib";

  public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
    serviceRegistryBuilder
        .applySetting("hibernate.connection.userAgent", HIBERNATE_API_CLIENT_LIB_TOKEN);
  }
}
