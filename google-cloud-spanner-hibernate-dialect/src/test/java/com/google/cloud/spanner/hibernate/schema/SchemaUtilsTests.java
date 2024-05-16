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

package com.google.cloud.spanner.hibernate.schema;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.hibernate.Interleaved;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import java.io.Serializable;
import org.junit.Test;

/** Tests for the Spanner schema utilities. */
public class SchemaUtilsTests {

  @Test
  public void childMissingEmbeddedId() {
    assertFalse(SchemaUtils.validateInterleaved(ChildMissingEmbeddedId.class));
  }

  @Test
  public void childIncompleteEmbeddedId() {
    assertFalse(SchemaUtils.validateInterleaved(ChildIncompleteEmbeddedId.class));
  }

  @Test
  public void validInterleavedTableStandardParent() {
    assertTrue(SchemaUtils.validateInterleaved(ValidChild.class));
  }

  @Test
  public void validInterleavedTableParentHeirarchy() {
    assertTrue(SchemaUtils.validateInterleaved(ValidChildParentHeirarchy.class));
  }

  @Test
  public void validChildUsingIdClass() {
    assertTrue(SchemaUtils.validateInterleaved(ValidChildWithIdClass.class));
  }

  @Test
  public void validNestedEmbeddableChild() {
    assertTrue(SchemaUtils.validateInterleaved(NestedChild.class));
  }

  @Test
  public void invalidCyclicalEmbeddableIds() {
    assertFalse(SchemaUtils.validateInterleaved(CyclicalNestedChild.class));
  }

  @Entity
  static class Parent {

    @Id @GeneratedValue String parentId;
  }

  @Entity
  static class ExtendedParent extends Parent {
    // Intentionally empty class
  }

  @Entity
  @Interleaved(parentEntity = Parent.class)
  static class ChildMissingEmbeddedId {

    // Intentionally no embedded Id
    @Id String childId;
  }

  @Entity
  @Interleaved(parentEntity = Parent.class)
  static class ChildIncompleteEmbeddedId {

    @EmbeddedId CompositeId id;

    static class CompositeId implements Serializable {
      // Intentionally no reference to the primary key in the Parent
      String childId;
    }
  }

  @Entity
  @Interleaved(parentEntity = Parent.class)
  static class ValidChild {

    @EmbeddedId CompositeId childId;

    static class CompositeId implements Serializable {
      String parentId;
      String childId;
    }
  }

  static class ChildId implements Serializable {
    @Id String parentId;

    @Id String childId;
  }

  @Entity
  @IdClass(value = ChildId.class)
  @Interleaved(parentEntity = Parent.class)
  static class ValidChildWithIdClass {
    @Id String parentId;

    @Id String childId;
  }

  @Entity
  @Interleaved(parentEntity = ExtendedParent.class)
  static class ValidChildParentHeirarchy {

    @EmbeddedId CompositeId childId;

    static class CompositeId implements Serializable {
      String parentId;
      String childId;
    }
  }

  // for testing nested embeddable ids
  static class GrandParent {
    @Id @GeneratedValue public long grandParentId;
  }

  @Interleaved(parentEntity = GrandParent.class)
  static class NestedParent {

    @EmbeddedId public ParentId parentId;

    @Embeddable
    static class ParentId implements Serializable {
      long grandParentId;
      long parentId;
    }
  }

  @Interleaved(parentEntity = NestedParent.class)
  static class NestedChild {

    @EmbeddedId public ChildId childId;

    @Embeddable
    static class ChildId implements Serializable {
      NestedParent.ParentId parentId;
      long childId;
    }
  }

  // For testing cyclical embeddable ids
  @Interleaved(parentEntity = CyclicalNestedChild.class)
  class CyclicalGrandParent {
    @EmbeddedId public GrandParentId grandParentId;

    @Embeddable
    class GrandParentId implements Serializable {
      CyclicalNestedChild.ChildId childId;
      long grandParentId;
    }
  }

  @Interleaved(parentEntity = CyclicalGrandParent.class)
  class CyclicalNestedParent {

    @EmbeddedId public ParentId parentId;

    @Embeddable
    class ParentId implements Serializable {
      CyclicalGrandParent.GrandParentId grandParentId;
      long parentId;
    }
  }

  @Interleaved(parentEntity = CyclicalNestedParent.class)
  class CyclicalNestedChild {

    @EmbeddedId public ChildId childId;

    @Embeddable
    class ChildId implements Serializable {
      CyclicalNestedParent.ParentId parentId;
      long childId;
    }
  }
}
