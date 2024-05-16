/*
 * Copyright 2019-2023 Google LLC
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

import com.google.cloud.spanner.Value;
import com.google.cloud.spanner.jdbc.JsonType;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/** A {@link JdbcType} for Spanner JSON columns. */
public class JsonSqlTypeDescriptor implements JdbcType {

  @Override
  public int getJdbcTypeCode() {
    return JsonType.VENDOR_TYPE_NUMBER;
  }

  @Override
  public <X> ValueBinder<X> getBinder(JavaType<X> javaTypeDescriptor) {
    return new BasicBinder<X>(javaTypeDescriptor, this) {
      @Override
      protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
          throws SQLException {
        JsonJavaTypeDescriptor typeDescriptor = (JsonJavaTypeDescriptor) javaTypeDescriptor;
        st.setObject(index, Value.json(typeDescriptor.unwrap(value, String.class, options)));
      }

      @Override
      protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) {
        throw new UnsupportedOperationException("Binding by name is not supported!");
      }
    };
  }

  @Override
  public <X> ValueExtractor<X> getExtractor(JavaType<X> javaTypeDescriptor) {
    return new BasicExtractor<X>(javaTypeDescriptor, this) {
      @Override
      protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options)
          throws SQLException {
        return javaTypeDescriptor.wrap(rs.getString(paramIndex), options);
      }

      @Override
      protected X doExtract(CallableStatement statement, int index, WrapperOptions options)
          throws SQLException {
        return javaTypeDescriptor.wrap(statement.getString(index), options);
      }

      @Override
      protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
          throws SQLException {
        return javaTypeDescriptor.wrap(statement.getString(name), options);
      }
    };
  }
}
