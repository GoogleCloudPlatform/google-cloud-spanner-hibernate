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

import com.google.cloud.spanner.hibernate.annotations.PooledBitReversedSequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Test entity that uses a pooled sequence. Pooled sequences are not supported in Cloud Spanner,
 * unless they use the custom {@link
 * com.google.cloud.spanner.hibernate.annotations.PooledBitReversedSequenceGenerator}.
 */
@Entity
public class PooledSequenceEntity {
  @Id
  @PooledBitReversedSequenceGenerator(
      sequenceName = "pooled_sequence",
      startWithCounter = 1,
      poolSize = 1000)
  private long id;

  @Column private String name;

  protected PooledSequenceEntity() {}

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
