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

import com.google.cloud.spanner.Type.Code;
import com.google.cloud.spanner.hibernate.reflection.ReflectionUtils;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.usertype.DynamicParameterizedType;

public class ArrayJavaTypeDescriptor
    extends AbstractTypeDescriptor<List<?>>
    implements DynamicParameterizedType {

  public static final ArrayJavaTypeDescriptor INSTANCE = new ArrayJavaTypeDescriptor();

  // The List type of the field set via reflection.
  private Class<?> spannerType = Object.class;
  private Code spannerTypeCode = Code.STRUCT;

  public ArrayJavaTypeDescriptor() {
    // This cast is needed to pass Object.class to the parent class
    super((Class<List<?>>)(Class<?>) List.class);
  }

  public Code getSpannerTypeCode() {
    return spannerTypeCode;
  }

  @Override
  public List<?> fromString(String string) {
    throw new UnsupportedOperationException("Creating a Java list from String is not supported.");
  }

  @Override
  public <X> X unwrap(List<?> value, Class<X> type, WrapperOptions options) {
    if (spannerType == Integer.class) {
      // If the value is a List<Integer>, convert it to List<Long> since Spanner only support INT64.
      value = ((List<Integer>) value).stream()
          .map(Integer::longValue)
          .collect(Collectors.toList());
    }

    return (X) value.toArray();
  }

  @Override
  public List<?> wrap(Object value, WrapperOptions options) {
    try {
      if (value instanceof Array) {
        Array sqlArray = (Array) value;
        return Arrays.asList(((Object[]) sqlArray.getArray()));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to convert SQL array type to a Java list: ", e);
    }

    throw new UnsupportedOperationException(
        "Unsupported type to convert: " + value.getClass()
            + " Java type descriptor only supports converting SQL array types.");
  }

  @Override
  public void setParameterValues(Properties parameters) {
    // Throw error if type is used on a non-List field.
    if (!parameters.get(DynamicParameterizedType.RETURNED_CLASS).equals(List.class.getName())) {
      String message = String.format(
          "Found invalid type annotation on field: %s. "
              + "The SpannerArrayListType must be applied on a java.util.List entity field.",
          parameters.get(DynamicParameterizedType.PROPERTY));

      throw new IllegalArgumentException(message);
    }

    // Get the class and the field name.
    Class<?> entityClass =
        ReflectionUtils.getClassOrFail(parameters.getProperty(DynamicParameterizedType.ENTITY));
    String fieldName = parameters.getProperty(DynamicParameterizedType.PROPERTY);

    // Get the parameterized type of the List<T>
    List<Class<?>> parameterizedTypes =
        ReflectionUtils.getParameterizedTypes(entityClass, fieldName);
    if (parameterizedTypes.isEmpty()) {
      throw new IllegalArgumentException(
          "You must specify an explicit parameterized type for your List type; i.e. List<Integer>");
    }
    Class<?> listType = parameterizedTypes.get(0);

    // Get the Spanner type string for the Java list type.
    spannerType = listType;
    spannerTypeCode = getSpannerTypeCode(listType);
  }

  /**
   * Maps a Java Class type to a Spanner Column type {@link Code}.
   *
   * <p>The type codes can be found in Spanner documentation:
   * https://cloud.google.com/spanner/docs/data-types#allowable_types
   */
  private static Code getSpannerTypeCode(Class<?> javaType) {
    if (Integer.class.isAssignableFrom(javaType)) {
      return Code.INT64;
    } else if (Long.class.isAssignableFrom(javaType)) {
      return Code.INT64;
    } else if (Double.class.isAssignableFrom(javaType)) {
      return Code.FLOAT64;
    } else if (String.class.isAssignableFrom(javaType)) {
      return Code.STRING;
    } else if (UUID.class.isAssignableFrom(javaType)) {
      return Code.STRING;
    } else if (Date.class.isAssignableFrom(javaType)) {
      return Code.TIMESTAMP;
    } else if (Boolean.class.isAssignableFrom(javaType)) {
      return Code.BOOL;
    } else if (BigDecimal.class.isAssignableFrom(javaType)) {
      return Code.NUMERIC;
    } else if (byte[].class.isAssignableFrom(javaType)) {
      return Code.BYTES;
    } else {
      throw new UnsupportedOperationException(
          "The " + javaType + " is not supported as a Spanner array type.");
    }
  }
}
