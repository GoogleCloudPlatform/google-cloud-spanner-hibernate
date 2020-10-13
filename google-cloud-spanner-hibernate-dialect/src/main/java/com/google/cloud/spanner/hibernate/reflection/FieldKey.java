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

import java.util.Objects;

public class FieldKey {
  private final Class<?> declaringClass;
  private final Class<?> type;
  private final String name;

  public FieldKey(Class<?> type, String name) {
    this(null, type, name);
  }

  public FieldKey(Class<?> declaringClass, Class<?> type, String name) {
    this.declaringClass = declaringClass;
    this.type = type;
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FieldKey other = (FieldKey) o;
    return Objects.equals(declaringClass, other.declaringClass)
        && Objects.equals(type, other.type)
        && Objects.equals(name, other.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(declaringClass, type, name);
  }

  @Override
  public String toString() {
    return "FieldKey{"
        + "declaringClass=" + declaringClass.toString()
        + "\n, type=" + type.toString()
        + "\n, name='" + name + '\''
        + "\n}";
  }
}