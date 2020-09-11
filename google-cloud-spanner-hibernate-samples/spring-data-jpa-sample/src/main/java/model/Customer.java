/*
 * Copyright 2019 Google LLC
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import org.hibernate.annotations.Type;

@Entity
public class Customer {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Type(type = "uuid-char")
  private UUID id;

  private String name;

  private String email;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "customer")
  private List<Coffee> coffees = new ArrayList<>();

  // Default constructor for Spring Data JPA.
  Customer() {

  }

  public Customer(String name, String email) {
    this.name = name;
    this.email = email;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public List<Coffee> getCoffees() {
    return coffees;
  }

  public void setCoffees(List<Coffee> coffees) {
    this.coffees = coffees;
  }
}
