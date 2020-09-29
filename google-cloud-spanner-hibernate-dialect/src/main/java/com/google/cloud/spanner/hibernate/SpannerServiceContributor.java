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
import java.util.Objects;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.hql.spi.id.inline.InlineIdsOrClauseBulkIdStrategy;
import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceContributor;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

/**
 * An implementation of a Hibernate {@link ServiceContributor} which provides custom settings
 * for the Spanner Hibernate dialect.
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
  public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
    if (Objects.equals(
        serviceRegistryBuilder.getSettings().get("hibernate.dialect"),
        SpannerDialect.class.getName())) {
      serviceRegistryBuilder
          // The custom Hibernate schema management tool for Spanner.
          .addInitiator(new StandardServiceInitiator() {
            @Override
            public Service initiateService(Map configurationValues,
                ServiceRegistryImplementor registry) {
              return SCHEMA_MANAGEMENT_TOOL;
            }
  
            @Override
            public Class getServiceInitiated() {
              return SchemaManagementTool.class;
            }
          })
          // The user agent JDBC connection property to identify the library.
          .applySetting("hibernate.connection.userAgent", HIBERNATE_API_CLIENT_LIB_TOKEN)
          // Create a unique index for a table if it does not already exist when in UPDATE mode.
          .applySetting(
              "hibernate.schema_update.unique_constraint_strategy",
              UniqueConstraintSchemaUpdateStrategy.RECREATE_QUIETLY)
          // Allows entities to be used with InheritanceType.JOINED in Spanner.
          .applySetting("hibernate.hql.bulk_id_strategy", InlineIdsOrClauseBulkIdStrategy.INSTANCE);
    }
  }
}
