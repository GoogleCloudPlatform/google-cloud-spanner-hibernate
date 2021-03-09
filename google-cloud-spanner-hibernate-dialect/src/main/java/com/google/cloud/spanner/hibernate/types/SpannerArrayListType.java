package com.google.cloud.spanner.hibernate.types;

import com.google.cloud.spanner.hibernate.types.internal.ArrayJavaTypeDescriptor;
import com.google.cloud.spanner.hibernate.types.internal.ArraySqlTypeDescriptor;
import java.util.List;
import java.util.Properties;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.usertype.DynamicParameterizedType;

public class SpannerArrayListType
    extends AbstractSingleColumnStandardBasicType<List<?>>
    implements DynamicParameterizedType {

  public SpannerArrayListType() {
    super(new ArraySqlTypeDescriptor(), new ArrayJavaTypeDescriptor());
  }

  @Override
  public String getName() {
    return "spanner-array-type";
  }

  @Override
  public void setParameterValues(Properties parameters) {
    ((ArrayJavaTypeDescriptor) getJavaTypeDescriptor()).setParameterValues(parameters);
  }
}
