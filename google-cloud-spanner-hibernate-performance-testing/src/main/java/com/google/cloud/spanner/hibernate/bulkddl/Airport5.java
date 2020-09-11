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

package com.google.cloud.spanner.hibernate.bulkddl;

import java.util.Date;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import org.hibernate.annotations.Type;

/**
 * A simple Hibernate entity for performance testing.
 */
@Entity
public class Airport5 {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Type(type = "uuid-char")
  private UUID id;

  private String name;

  private String address;

  private String country;

  private Date dateBuilt;

  private long planeCapacity;

  /**
   * Constructs the Airport Hibernate test entity.
   */
  public Airport5(String name, String address, String country, Date dateBuilt, long planeCapacity) {
    this.name = name;
    this.address = address;
    this.country = country;
    this.dateBuilt = dateBuilt;
    this.planeCapacity = planeCapacity;
  }

  /**
   * Default entity constructor for Hibernate.
   */
  public Airport5() {

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

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public Date getDateBuilt() {
    return dateBuilt;
  }

  public void setDateBuilt(Date dateBuilt) {
    this.dateBuilt = dateBuilt;
  }

  public long getPlaneCapacity() {
    return planeCapacity;
  }

  public void setPlaneCapacity(long planeCapacity) {
    this.planeCapacity = planeCapacity;
  }
}
