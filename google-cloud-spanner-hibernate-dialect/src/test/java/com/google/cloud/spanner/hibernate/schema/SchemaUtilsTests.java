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

package com.google.cloud.spanner.hibernate.schema;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.hibernate.Interleaved;
import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.IdClass;
import org.junit.Test;

/**
 * Tests for the Spanner schema utilities.
 */
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
  class Parent {

    @Id
    @GeneratedValue
    String parentId;
  }

  @Entity
  class ExtendedParent extends Parent {
    // Intentionally empty class
  }

  @Entity
  @Interleaved(parentEntity = Parent.class)
  class ChildMissingEmbeddedId {

    // Intentionally no embedded Id
    @Id
    String childId;
  }

  @Entity
  @Interleaved(parentEntity = Parent.class)
  class ChildIncompleteEmbeddedId {

    @EmbeddedId
    CompositeId id;

    class CompositeId {
      // Intentionally no reference to the primary key in the Parent
      String childId;
    }
  }

  @Entity
  @Interleaved(parentEntity = Parent.class)
  class ValidChild {

    @EmbeddedId
    CompositeId childId;

    class CompositeId {
      String parentId;
      String childId;
    }
  }

  class ChildId {
    @Id
    String parentId;

    @Id
    String childId;
  }

  @Entity
  @IdClass(value = ChildId.class)
  @Interleaved(parentEntity = Parent.class)
  class ValidChildWithIdClass {
    @Id
    String parentId;

    @Id
    String childId;
  }

  @Entity
  @Interleaved(parentEntity = ExtendedParent.class)
  class ValidChildParentHeirarchy {

    @EmbeddedId
    CompositeId childId;

    class CompositeId {
      String parentId;
      String childId;
    }
  }

  // for testing nested embeddable ids
  class GrandParent {
    @Id
    @GeneratedValue
    public long grandParentId;
  }

  @Interleaved(parentEntity = GrandParent.class)
  class NestedParent {

    @EmbeddedId
    public ParentId parentId;

    @Embeddable
    class ParentId implements Serializable {
      long grandParentId;
      long parentId;
    }
  }

  @Interleaved(parentEntity = NestedParent.class)
  class NestedChild {

    @EmbeddedId
    public ChildId childId;

    @Embeddable
    class ChildId implements Serializable {
      NestedParent.ParentId parentId;
      long childId;
    }
  }

  // For testing cyclical embeddable ids
  @Interleaved(parentEntity = CyclicalNestedChild.class)
  class CyclicalGrandParent {
    @EmbeddedId
    public GrandParentId grandParentId;

    @Embeddable
    class GrandParentId implements Serializable {
      CyclicalNestedChild.ChildId childId;
      long grandParentId;
    }
  }

  @Interleaved(parentEntity = CyclicalGrandParent.class)
  class CyclicalNestedParent {

    @EmbeddedId
    public ParentId parentId;

    @Embeddable
    class ParentId implements Serializable {
      CyclicalGrandParent.GrandParentId grandParentId;
      long parentId;
    }
  }

  @Interleaved(parentEntity = CyclicalNestedParent.class)
  class CyclicalNestedChild {

    @EmbeddedId
    public ChildId childId;

    @Embeddable
    class ChildId implements Serializable {
      CyclicalNestedParent.ParentId parentId;
      long childId;
    }
  }
}
