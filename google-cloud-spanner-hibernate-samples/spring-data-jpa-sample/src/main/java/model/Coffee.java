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

package model;

import com.google.cloud.spanner.hibernate.PooledBitReversedSequenceStyleGenerator;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
public class Coffee {

  /**
   * This entity uses a bit-reversed sequence to generate identifiers. See {@link
   * PooledBitReversedSequenceStyleGenerator} for more information.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "coffee_id_generator")
  @GenericGenerator(
      name = "coffee_id_generator",
      type = PooledBitReversedSequenceStyleGenerator.class,
      parameters = {
        @Parameter(name = "sequence_name", value = "coffee_id"),
        @Parameter(name = "increment_size", value = "200")
      })
  private Long id;

  private String size;

  @ManyToOne private Customer customer;

  // Empty default constructor for Spring Data JPA.
  public Coffee() {}

  public Coffee(Customer customer, String size) {
    this.customer = customer;
    this.size = size;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public Customer getCustomer() {
    return customer;
  }

  public void setCustomer(Customer customer) {
    this.customer = customer;
  }
}
