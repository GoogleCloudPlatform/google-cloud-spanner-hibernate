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

package com.example;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.hibernate.annotations.Type;


/**
 * An example person entity.
 *
 * @author Chengyuan Zhao
 */
@Entity(name = "Person")
// [START spanner_hibernate_table_name]
@Table(name = "PersonsTable")
// [END spanner_hibernate_table_name]
public class Person {

  // [START spanner_hibernate_generated_ids]
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Type(type = "uuid-char")
  private UUID id;
  // [END spanner_hibernate_generated_ids]

  private String name;

  private String nickName;

  private String address;

  // An example of an entity relationship.
  @OneToOne
  private Payment payment;

  public Person() {}

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

  public String getNickName() {
    return nickName;
  }

  public void setNickName(String nickName) {
    this.nickName = nickName;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public Payment getPayment() {
    return payment;
  }

  public void setPayment(Payment payment) {
    this.payment = payment;
  }

  @Override
  public String toString() {
    return "Person{" +
        "\n id=" + id +
        "\n name='" + name + '\'' +
        "\n nickName='" + nickName + '\'' +
        "\n address='" + address + '\'' +
        "\n payment_amount=" + payment.getAmount() +
        "\n}";
  }
}
