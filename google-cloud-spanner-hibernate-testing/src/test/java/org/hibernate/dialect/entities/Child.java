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
import org.hibernate.dialect.entities.Parent.ParentId;

@Entity
@Interleaved(parentEntity = Parent.class)
public class Child {

  @EmbeddedId public ChildId childId;

  public String name;

  public void setChildId(ChildId childId) {
    this.childId = childId;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Embeddable
  public static class ChildId implements Serializable {

    ParentId parentId;

    public long childId;

    public ChildId(ParentId parentId, long childId) {
      this.parentId = parentId;
      this.childId = childId;
    }

    public ChildId() {}

    @Override
    public String toString() {
      return "ChildId{" + "parentId=" + parentId + ", childId=" + childId + '}';
    }
  }

  @Override
  public String toString() {
    return "Child{" + "childId=" + childId + ", name='" + name + '\'' + '}';
  }
}
