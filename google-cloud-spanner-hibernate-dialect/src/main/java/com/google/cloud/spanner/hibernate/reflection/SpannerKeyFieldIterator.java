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

package com.google.cloud.spanner.hibernate.reflection;

import java.lang.reflect.Field;
import java.util.Iterator;

/**
 * Used for iterating over the declared fields on a given class definition. This includes
 * private/protected fields and fields in any superclasses. However, this relies on
 * {@link java.lang.Class#getDeclaredFields()} and as such, iteration order is not guaranteed.
 * Iteration will not include synthetic fields and can throw SecurityExceptions based on configured
 * SecurityManager
 *
 * @see java.lang.SecurityManager
 */
public class SpannerKeyFieldIterator implements Iterator<Field> {
  private int index;
  private Class<?> clazz;
  private Field[] fields;

  /**
   * Constructor.
   */
  public SpannerKeyFieldIterator(Class<?> clazz) {
    this.clazz = clazz;
  }

  @Override
  public boolean hasNext() throws SecurityException {
    while (hasNextPvt()) {
      if (!fields[index].isSynthetic()) {
        return true;
      }
      ++index;
    }

    return false;
  }

  private boolean hasNextPvt() {
    if (fields == null) {
      fields = clazz.getDeclaredFields();
      index = 0;
    }

    if (index >= fields.length) {
      clazz = clazz.getSuperclass();
      if (null != clazz) {
        fields = clazz.getDeclaredFields();
        index = 0;
      }
    }
    return null != clazz && index < fields.length;
  }

  @Override
  public Field next() {
    Field field = fields[index];
    ++index;
    return field;
  }

  /**
   * Build an iterable wrapping this key field iterator.
   */
  public static Iterable<Field> iterable(Class<?> clazz) {
    return () -> new SpannerKeyFieldIterator(clazz);
  }
}
