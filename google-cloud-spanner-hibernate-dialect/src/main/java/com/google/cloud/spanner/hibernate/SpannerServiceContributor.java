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

import com.google.cloud.spanner.hibernate.schema.SpannerSchemaManagementTool;
import java.util.Map;
import javax.annotation.Nonnull;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.spi.ServiceContributor;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.UniqueConstraintSchemaUpdateStrategy;
import org.hibernate.tool.schema.internal.SchemaManagementToolInitiator;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

/**
 * An implementation of a Hibernate {@link ServiceContributor} which provides custom settings for
 * the Spanner Hibernate dialect.
 *
 * <p>Note that Hibernate will automatically pass down all "hibernate.connection.*" properties
 * without the prefix to {@code Driver.connect(url, props)}.
 *
 * @author Mike Eltsufin
 */
public class SpannerServiceContributor implements ServiceContributor {

  private static final SpannerSchemaManagementTool SCHEMA_MANAGEMENT_TOOL =
      new SpannerSchemaManagementTool();

  static final String HIBERNATE_API_CLIENT_LIB_TOKEN = "sp-hib";

  @Override
  public void contribute(@Nonnull StandardServiceRegistryBuilder serviceRegistryBuilder) {
    serviceRegistryBuilder
        // The custom Hibernate schema management tool for Spanner.
        .addInitiator(
            new SchemaManagementToolInitiator() {
              @Override
              public SchemaManagementTool initiateService(
                  Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
                JdbcEnvironment jdbcEnvironment = registry.getService(JdbcEnvironment.class);
                if (jdbcEnvironment != null) {
                  if (SpannerDialect.class.isAssignableFrom(
                      jdbcEnvironment.getDialect().getClass())) {
                    return SCHEMA_MANAGEMENT_TOOL;
                  }
                }
                return super.initiateService(configurationValues, registry);
              }
            })
        // The user agent JDBC connection property to identify the library.
        .applySetting("hibernate.connection.userAgent", HIBERNATE_API_CLIENT_LIB_TOKEN)
        // Adding this setting prevents Hibernate from dropping and re-creating (unique) indexes
        // every time a session factory is created with hbm2dll=update.
        .applySetting("hibernate.schema_update.unique_constraint_strategy",
                      UniqueConstraintSchemaUpdateStrategy.RECREATE_QUIETLY);
  }
}