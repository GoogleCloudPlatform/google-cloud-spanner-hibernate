package com.google.cloud.spanner.hibernate.reflection;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class ReflectionUtilsTests {

  private static String CLASS_NAME =
      "com.google.cloud.spanner.hibernate.reflection.ReflectionUtilsTests$SimpleClass";

  @Test
  public void testGetClass() {
    Class<?> clazz = ReflectionUtils.getClassOrFail(CLASS_NAME);
    assertThat(clazz).isEqualTo(SimpleClass.class);
  }

  @Test
  public void testGetField() {
    Field field = ReflectionUtils.getFieldOrFail(
        ReflectionUtils.getClassOrFail(CLASS_NAME), "number");
    assertThat(field.getType()).isEqualTo(int.class);
  }

  @Test
  public void testGetGenericType() {
    List<Class<?>> paramType = ReflectionUtils.getParameterizedTypes(
        ReflectionUtils.getClassOrFail(CLASS_NAME), "numberList");
    assertThat(paramType).containsExactly(Integer.class);
  }

  private static class SimpleClass {

    public int number = 0;

    public List<Integer> numberList = Collections.EMPTY_LIST;
  }
}
