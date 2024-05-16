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

package com.google.cloud.spanner.hibernate.entities;

import com.google.cloud.spanner.hibernate.Interleaved;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import java.io.Serializable;

/** Hibernate entity used in interleaved tests. */
@Entity
@Interleaved(parentEntity = Parent.class)
public class Child {

  @EmbeddedId public ChildId childId;

  public String name;

  /** Embedded ID containing the interleaved parents' fields. */
  @Embeddable
  public static class ChildId implements Serializable {

    public long grandParentId;

    public long parentId;

    public long childId;
  }
}
