/*
 * Copyright 2023 Google LLC
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

package org.hibernate.dialect.entities;

import com.google.cloud.spanner.hibernate.Interleaved;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import java.io.Serializable;

@Entity
@Interleaved(parentEntity = GrandParent.class)
public class Parent {

  @EmbeddedId public ParentId parentId;

  public String name;

  public void setParentId(ParentId parentId) {
    this.parentId = parentId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ParentId getParentId() {
    return parentId;
  }

  public String getName() {
    return name;
  }

  @Embeddable
  public static class ParentId implements Serializable {

    public long grandParentId;

    public long parentId;

    public ParentId(long grandParentId, long parentId) {
      this.grandParentId = grandParentId;
      this.parentId = parentId;
    }

    public ParentId() {}

    @Override
    public String toString() {
      return "ParentId{" + "grandParentId=" + grandParentId + ", parentId=" + parentId + '}';
    }
  }

  @Override
  public String toString() {
    return "Parent{" + "parentId=" + parentId + ", name='" + name + '\'' + '}';
  }
}
