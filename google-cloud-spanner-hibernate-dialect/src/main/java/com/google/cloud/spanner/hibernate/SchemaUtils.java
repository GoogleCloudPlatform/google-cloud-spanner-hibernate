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

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

/**
 * Schema utilities for reading table {@link Metadata} in Hibernate.
 */
public class SchemaUtils {

  private SchemaUtils() {
  }

  /**
   * Returns the {@link Interleaved} annotation on a table if it exists.
   */
  public static Interleaved getInterleaveAnnotation(Table table, Metadata metadata) {
    for (PersistentClass pc : metadata.getEntityBindings()) {
      if (pc.getTable().equals(table) && pc.getMappedClass() != null) {
        Class<?> entityClass = pc.getMappedClass();
        return entityClass.getAnnotation(Interleaved.class);
      }
    }

    return null;
  }

  /**
   * Gets the Spanner {@link Table} by name.
   */
  public static Table getTable(Class<?> entityClass, Metadata metadata) {
    for (PersistentClass pc : metadata.getEntityBindings()) {
      if (pc.getMappedClass().equals(entityClass)) {
        return pc.getTable();
      }
    }

    throw new IllegalArgumentException(
        String.format("Could not find table for entity class %s.", entityClass.getName()));
  }
}
