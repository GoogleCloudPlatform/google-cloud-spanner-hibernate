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

package com.google.cloud.spanner.hibernate.types.internal;

import com.google.gson.Gson;
import java.util.Properties;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.usertype.DynamicParameterizedType;

/**
 * A {@link AbstractTypeDescriptor} for Spanner JSON columns.
 */
public class JsonJavaTypeDescriptor extends AbstractTypeDescriptor<Object>
    implements DynamicParameterizedType {

  private static final Gson gson = new Gson();

  // The JSON type declared in the entity.
  private Class<?> propertyType = Object.class;

  public JsonJavaTypeDescriptor() {
    // This cast is needed to pass Object.class to the parent class
    super(Object.class);
  }

  @Override
  public Object fromString(String input) {
    if (String.class.isAssignableFrom(propertyType)) {
      return input;
    }
    return gson.fromJson(input, propertyType);
  }

  @Override
  public <X> X unwrap(Object value, Class<X> type, WrapperOptions options) {
    return (X) gson.toJson(value);
  }

  @Override
  public <X> Object wrap(X value, WrapperOptions options) {
    if (value instanceof String) {
      return fromString((String) value);
    }

    throw new UnsupportedOperationException(
        "Unable to convert type " + value.getClass() + " to JSON.");
  }

  @Override
  public void setParameterValues(Properties parameters) {
    try {
      // The entity class to convert JSON into.
      propertyType = Class.forName(parameters.getProperty(DynamicParameterizedType.RETURNED_CLASS));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Failed to map JSON entity column.", e);
    }
  }
}
