package com.google.cloud.spanner.hibernate.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides helper functions to do Java Reflection.
 */
public class ReflectionUtils {

  /**
   * Gets the specified class name on the classpath.
   */
  public static Class<?> getClassOrFail(String className) {
    try {
      return Class.forName(
          className, false, Thread.currentThread().getContextClassLoader());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Failed to find class: " + className, e);
    }
  }

  /**
   * Gets the {@link Field} of a class by name.
   */
  public static Field getFieldOrFail(Class<?> clazz, String fieldName) {
    try {
      return clazz.getDeclaredField(fieldName);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException("Failed to find field: " + fieldName, e);
    }
  }

  /**
   * Returns the list of parameterized types of a field if it has one, else returns null.
   *
   * For example, a field with type {@code List<Integer>} would return [Integer].
   * On the other hand, a field with no generic type such as "Long" returns the empty list.
   */
  public static List<Class<?>> getParameterizedTypes(Class<?> clazz, String fieldName) {
    Field field = getFieldOrFail(clazz, fieldName);

    Type genericType = field.getGenericType();
    if (genericType instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) genericType;

      // Return the list of parameterized types.
      return Arrays.stream(parameterizedType.getActualTypeArguments())
          .map(type -> getClassOrFail(type.getTypeName()))
          .collect(Collectors.toList());
    } else {
      return Collections.emptyList();
    }
  }
}
