package com.google.cloud.spanner.hibernate.types.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

public class ArraySqlTypeDescriptor implements SqlTypeDescriptor {

  public static final ArraySqlTypeDescriptor INSTANCE = new ArraySqlTypeDescriptor();

  @Override
  public int getSqlType() {
    return Types.ARRAY;
  }

  @Override
  public boolean canBeRemapped() {
    return true;
  }

  @Override
  public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
    return new BasicBinder<X>(javaTypeDescriptor, this) {
      @Override
      protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
          throws SQLException {
        ArrayJavaTypeDescriptor arrayJavaTypeDescriptor =
            (ArrayJavaTypeDescriptor) javaTypeDescriptor;
        st.setArray(index, st.getConnection().createArrayOf(
            arrayJavaTypeDescriptor.getArrayType(),
            null
            // arrayJavaTypeDescriptor.unwrap(value, Object[].class, options)
        ));
      }

      @Override
      protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
          throws SQLException {
        throw new UnsupportedOperationException("Binding by name is not supported!");
      }
    };
  }

  @Override
  public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
    return null;
  }
}
