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

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * Tests for the Reflection utilities.
 */
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
