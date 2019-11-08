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
        : options.get( AvailableSettings.HBM2DDL_FILTER_PROVIDER );

    return getServiceRegistry().getService(StrategySelector.class)
        .resolveDefaultableStrategy(
            SchemaFilterProvider.class,
            configuredOption,
            DefaultSchemaFilterProvider.INSTANCE);
  }
}
