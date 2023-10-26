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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

/**
 * Test entity for using a bit-reversed sequence that supports batching.
 */
@Entity
public class PooledBitReversedSequenceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "batch_bit_reversed_generator")
  @GenericGenerator(name = "batch_bit_reversed_generator",
      strategy = "com.google.cloud.spanner.hibernate.PooledBitReversedSequenceStyleGenerator",
      parameters = {
          @Parameter(name = "sequence_name", value = "enhanced_sequence"),
          @Parameter(name = "increment_size", value = "5"),
          @Parameter(name = "initial_value", value = "5000"),
          @Parameter(name = "exclude_range", value = "[1,1000]")})
  private long id;

  @Column
  private String name;

  public PooledBitReversedSequenceEntity() {
  }

  public PooledBitReversedSequenceEntity(String name) {
    this.name = name;
  }

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
