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

package com.example.entities;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * Hibernate entity demonstrating inheritance.
 */
@Entity
public class WireTransferPayment extends Payment {

  @Column(name = "wire_id")
  private String wireId;

  public String getWireId() {
    return wireId;
  }

  public void setWireId(String wireId) {
    this.wireId = wireId;
  }
}
