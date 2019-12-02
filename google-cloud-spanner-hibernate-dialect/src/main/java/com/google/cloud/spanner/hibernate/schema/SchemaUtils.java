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

import com.google.cloud.spanner.hibernate.Interleaved;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

/**
 * Schema utilities for reading table {@link Metadata} in Hibernate.
 */
class SchemaUtils {

  private SchemaUtils() {
  }

  /**
   * Returns the {@link Interleaved} annotation on a table if it exists.
   */
  public static Interleaved getInterleaveAnnotation(Table table, Metadata metadata) {
    for (PersistentClass pc : metadata.getEntityBindings()) {
      if (pc.getTable().equals(table) && pc.getMappedClass() != null) {
        Class<?> entityClass = pc.getMappedClass();

        Interleaved result = entityClass.getAnnotation(Interleaved.class);
        if (result != null && result.parentEntity().equals(void.class)) {
          throw new IllegalArgumentException(
              "Please specify a interleaved parentEntity for entity " + entityClass.getName());
        }
        return result;
      }
    }

    return null;
  }

  /**
   * Gets the Spanner {@link Table} by entity class.
   */
  public static Table getTable(Class<?> entityClass, Metadata metadata) {
    PersistentClass pc = metadata.getEntityBinding(entityClass.getCanonicalName());
    if (pc != null) {
      return pc.getTable();
    }

    throw new IllegalArgumentException(
        String.format("Could not find table for entity class %s.", entityClass.getName()));
  }
}
