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

package com.google.cloud.spanner.hibernate.types;

import com.google.cloud.spanner.hibernate.types.internal.JsonJavaTypeDescriptor;
import com.google.cloud.spanner.hibernate.types.internal.JsonSqlTypeDescriptor;
import java.util.Properties;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.usertype.DynamicParameterizedType;

/**
 * A {@link org.hibernate.usertype.UserType} which supports mapping Spanner JSON columns to entity
 * fields.
 */
public class SpannerJsonType extends AbstractSingleColumnStandardBasicType<Object>
    implements DynamicParameterizedType {

  public SpannerJsonType() {
    super(new JsonSqlTypeDescriptor(), new JsonJavaTypeDescriptor());
  }

  @Override
  public String getName() {
    return "spanner-json-type";
  }

  @Override
  public void setParameterValues(Properties parameters) {
    ((JsonJavaTypeDescriptor) getJavaTypeDescriptor()).setParameterValues(parameters);
  }
}
