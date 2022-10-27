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

package com.google.cloud.spanner.hibernate.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

/**
 * A test entity that uses a bit-reversed sequence for ID generation.
 *
 * @author loite
 */
@Entity
public class Customer {
  /**
   * This ID generator simulates a bit-reversed sequence with an increment size of 1,000 and a start
   * value of 50,000.
   *
   * <ol>
   *   <li>Starts at 50,000
   *   <li>Increases by 1,000 each time next_val is called
   *   <li>The returned value is bit-reversed, meaning that the value will not be monotonically
   *       increasing
   * </ol>
   */
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customerId")
  @GenericGenerator(
      name = "customerId",
      strategy = "com.google.cloud.spanner.hibernate.BitReversedSequenceStyleGenerator",
      parameters = {
        @Parameter(name = SequenceStyleGenerator.INCREMENT_PARAM, value = "1000"),
        @Parameter(name = SequenceStyleGenerator.SEQUENCE_PARAM, value = "customerId"),
        @Parameter(name = SequenceStyleGenerator.INITIAL_PARAM, value = "50000")
      })
  @Column(nullable = false)
  private Long customerId;

  private String name;
}
