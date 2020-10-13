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

public class FieldIteratorTests {

  @Test
  public void simpleClassFieldIteration() {
    Set<FieldKey> fields = new HashSet<>();
    for (Field field: new FieldIterable(Parent.class)) {
      fields.add(new FieldKey(field.getDeclaringClass(), field.getType(), field.getName()));
    }

    Set<FieldKey> expected = new HashSet<>();
    expected.add(new FieldKey(Parent.class, String.class, "publicId"));
    expected.add(new FieldKey(Parent.class, String.class, "protectedId"));
    expected.add(new FieldKey(Parent.class, String.class, "privateId"));
    expected.add(new FieldKey(Parent.class, int.class, "privatePrimitive"));

    assertEquals(expected, fields);
  }

  @Test
  public void subclassFieldIteration() {
    Set<FieldKey> fields = new HashSet<>();
    for (Field field: new FieldIterable(ParentSubClass.class)) {
      fields.add(new FieldKey(field.getDeclaringClass(), field.getType(), field.getName()));
    }

    Set<FieldKey> expected = new HashSet<>();
    expected.add(new FieldKey(Parent.class, String.class, "publicId"));
    expected.add(new FieldKey(Parent.class, String.class, "protectedId"));
    expected.add(new FieldKey(Parent.class, String.class, "privateId"));
    expected.add(new FieldKey(Parent.class, int.class, "privatePrimitive"));
    expected.add(new FieldKey(ParentSubClass.class, String.class, "someOtherField"));
    expected.add(new FieldKey(ParentSubClass.class, int.class, "privatePrimitive"));

    assertEquals(expected, fields);
  }

  @Test
  public void genericClassImplIteration() {
    Set<FieldKey> fields = new HashSet<>();
    for (Field field: new FieldIterable(GenericParentImpl.class)) {
      fields.add(new FieldKey(field.getDeclaringClass(), field.getType(), field.getName()));
    }

    // Due to type erasure the generic field will still be of type 'Object'
    Set<FieldKey> expected = new HashSet<>();
    expected.add(new FieldKey(GenericParent.class, Object.class, "genericField"));
    expected.add(new FieldKey(GenericParentImpl.class, String.class, "someOtherField"));

    assertEquals(expected, fields);
  }
}
