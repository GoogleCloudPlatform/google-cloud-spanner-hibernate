/*
 * Copyright 2019-2024 Google LLC
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

import com.google.cloud.spanner.Type.Code;
import java.io.Serializable;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

/** Base class for Spanner ARRAY types. */
public abstract class AbstractSpannerArrayType<A, T> implements UserType<List<T>> {

  @Override
  public Class returnedClass() {
    return List.class;
  }

  @Override
  public int getSqlType() {
    return Types.ARRAY;
  }

  public abstract Code getSpannerTypeCode();

  public String getArrayElementTypeName() {
    return getSpannerTypeCode().name();
  }

  public abstract A[] toArray(List<T> value);
  
  public List<T> toList(java.sql.Array array) throws SQLException {
    return Arrays.asList((T[]) array.getArray());
  }

  @Override
  public List<T> nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session,
      Object owner) throws SQLException {
    Array array = rs.getArray(position);
    return array != null ? toList(array) : null;
  }

  @Override
  public void nullSafeSet(PreparedStatement st, List<T> value, int index,
      SharedSessionContractImplementor session) throws SQLException {
    if (st != null) {
      if (value != null) {
        session.doWork(connection -> {
          Array array = connection.createArrayOf(getArrayElementTypeName(), toArray(value));
          st.setArray(index, array);
        });
      } else {
        st.setNull(index, Types.ARRAY);
      }
    }
  }

  @Override
  public ArrayList<T> deepCopy(List<T> value) {
    if (value == null) {
      return null;
    }
    return new ArrayList<>(value);
  }

  @Override
  public boolean isMutable() {
    return true;
  }

  @Override
  public Serializable disassemble(List<T> value) {
    return deepCopy(value);
  }

  @Override
  public List<T> assemble(Serializable cached, Object owner) {
    return deepCopy((ArrayList<T>) cached);
  }

  @Override
  public boolean equals(List<T> x, List<T> y) {
    if (x == null && y == null) {
      return true;
    }
    if (x == null || y == null) {
      return false;
    }
    return x.equals(y);
  }

  @Override
  public int hashCode(List<T> x) {
    if (x == null) {
      return 0;
    }
    return x.hashCode();
  }
}
