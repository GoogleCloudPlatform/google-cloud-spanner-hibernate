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

package com.google.cloud.spanner.hibernate.schema;

import java.util.Map;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.schema.internal.DefaultSchemaFilterProvider;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;

public class SpannerSchemaManagementTool extends HibernateSchemaManagementTool {

  @Override
  public SchemaCreator getSchemaCreator(Map options) {
    return new SpannerSchemaCreator(this, getSchemaFilterProvider(options).getCreateFilter());
  }

  @Override
  public SchemaDropper getSchemaDropper(Map options) {
    return new SpannerSchemaDropper(this, getSchemaFilterProvider(options).getCreateFilter());
  }

  /**
   * Returns the {@link SchemaFilterProvider} used for creating the Schema creator and dropper.
   */
  private SchemaFilterProvider getSchemaFilterProvider(Map options) {
    final Object configuredOption = (options == null)
        ? null
        : options.get(AvailableSettings.HBM2DDL_FILTER_PROVIDER);

    return getServiceRegistry().getService(StrategySelector.class)
        .resolveDefaultableStrategy(
            SchemaFilterProvider.class,
            configuredOption,
            DefaultSchemaFilterProvider.INSTANCE);
  }
}
