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

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/**
 * Tests verifying that the SpannerKeyFieldIterator behaves as expected.
 *
 * Note - Since the test classes are defined inline, each class in the heirarchy hold references to
 * 'his$0'
 */
public class SpannerKeyFieldIteratorTests {

  @Test
  public void simpleClassFieldIteration() {
    Set<SpannerEntityFieldKey> fields = new HashSet<>();
    for (Field field: SpannerKeyFieldIterator.iterable(Parent.class)) {
      fields.add(new SpannerEntityFieldKey(field.getDeclaringClass(), field.getType(), field.getName()));
    }

    Set<SpannerEntityFieldKey> expected = new HashSet<>();

    expected.add(new SpannerEntityFieldKey(Parent.class, getClass(), "this$0"));
    expected.add(new SpannerEntityFieldKey(Parent.class, String.class, "publicId"));
    expected.add(new SpannerEntityFieldKey(Parent.class, String.class, "protectedId"));
    expected.add(new SpannerEntityFieldKey(Parent.class, String.class, "privateId"));
    expected.add(new SpannerEntityFieldKey(Parent.class, int.class, "privatePrimitive"));

    assertEquals(expected, fields);
  }

  @Test
  public void subclassFieldIteration() {
    Set<SpannerEntityFieldKey> fields = new HashSet<>();
    for (Field field: SpannerKeyFieldIterator.iterable(ParentSubClass.class)) {
      fields.add(new SpannerEntityFieldKey(field.getDeclaringClass(), field.getType(), field.getName()));
    }

    Set<SpannerEntityFieldKey> expected = new HashSet<>();
    expected.add(new SpannerEntityFieldKey(Parent.class, String.class, "publicId"));
    expected.add(new SpannerEntityFieldKey(Parent.class, String.class, "protectedId"));
    expected.add(new SpannerEntityFieldKey(Parent.class, String.class, "privateId"));
    expected.add(new SpannerEntityFieldKey(Parent.class, int.class, "privatePrimitive"));
    expected.add(new SpannerEntityFieldKey(Parent.class, getClass(), "this$0"));
    expected.add(new SpannerEntityFieldKey(ParentSubClass.class, getClass(), "this$0"));
    expected.add(new SpannerEntityFieldKey(ParentSubClass.class, String.class, "someOtherField"));
    expected.add(new SpannerEntityFieldKey(ParentSubClass.class, int.class, "privatePrimitive"));

    assertEquals(expected, fields);
  }

  @Test
  public void genericClassImplIteration() {
    Set<SpannerEntityFieldKey> fields = new HashSet<>();
    for (Field field: SpannerKeyFieldIterator.iterable(GenericParentImpl.class)) {
      fields.add(new SpannerEntityFieldKey(field.getDeclaringClass(), field.getType(), field.getName()));
    }

    // Due to type erasure the generic field will still be of type 'Object'
    Set<SpannerEntityFieldKey> expected = new HashSet<>();
    expected.add(new SpannerEntityFieldKey(GenericParent.class, Object.class, "genericField"));
    expected.add(new SpannerEntityFieldKey(GenericParent.class, getClass(), "this$0"));
    expected.add(new SpannerEntityFieldKey(GenericParentImpl.class, getClass(), "this$0"));
    expected.add(new SpannerEntityFieldKey(GenericParentImpl.class, String.class, "someOtherField"));

    assertEquals(expected, fields);
  }

  class Parent {
    public String publicId;
    protected String protectedId;
    private String privateId;
    private int privatePrimitive;
  }

  class ParentSubClass extends Parent {
    private String someOtherField;
    private int privatePrimitive;
  }

  class GenericParent<T> {
    private T genericField;
  }

  class GenericParentImpl extends GenericParent<String> {
    private String someOtherField;
  }
}
