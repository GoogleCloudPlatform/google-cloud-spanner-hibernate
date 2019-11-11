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

package com.google.cloud.spanner.hibernate.bulkddl;

import com.google.cloud.spanner.hibernate.BenchmarkUtil;
import com.google.cloud.spanner.hibernate.ClientLibraryOperations;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;
import org.hibernate.cfg.Configuration;

/**
 * A stand-alone driver to run a bulk DDL operation (creates 10 tables).
 *
 * <p>This is kept separate from the other performance testing code in order to keep the bulk DDL
 * entities organized from the rest of the performance testing code.
 */
public class BulkDdlDriver {

  private static final Logger LOGGER = Logger.getLogger(BulkDdlDriver.class);

  private static final List<Class<?>> ENTITY_CLASSES = Arrays.asList(
      Airport1.class, Airport2.class, Airport3.class, Airport4.class, Airport5.class,
      Airport6.class, Airport7.class, Airport8.class, Airport9.class, Airport10.class);

  /**
   * Runs and benchmarks a bulk DDL operation.
   */
  public static void main(String[] args) {
    LOGGER.info("Resetting the Spanner test database.");
    ClientLibraryOperations clientLibraryOperations = new ClientLibraryOperations();
    clientLibraryOperations.resetTestDatabase();

    Configuration configuration = new Configuration()
        .configure("hibernate.cfg.xml")
        .setProperty("hibernate.show_sql", "true");

    for (Class<?> entityClass : ENTITY_CLASSES) {
      configuration.addAnnotatedClass(entityClass);
    }

    // Spanner tables are created as a side-effect of configuration.buildSessionFactory().
    BenchmarkUtil.benchmark(
        () -> configuration.buildSessionFactory(),
        "Creating 10 tables using hibernate.");
  }
}
