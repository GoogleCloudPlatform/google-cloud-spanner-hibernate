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

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import java.sql.Types;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;

/**
 * A Test entity with JSON entity fields.
 */
// TODO: User-defined types need a re-implementation for Hibernate 6.
//@TypeDefs({
//    @TypeDef(
//        name = "json",
//        typeClass = SpannerJsonType.class
//    )
//})
@Entity
public class JsonEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @JdbcTypeCode(Types.CHAR)
  private UUID id;

  // TODO: Update mapping to use JSON
  @Transient
  private Employee employee;

  public UUID getId() {
    return id;
  }

  public Employee getEmployee() {
    return employee;
  }

  public void setEmployee(Employee employee) {
    this.employee = employee;
  }

  public static class Employee {
    public String name;
    public Address address;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Address getAddress() {
      return address;
    }

    public void setAddress(Address address) {
      this.address = address;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Employee)) {
        return false;
      }
      Employee employee = (Employee) o;
      return Objects.equals(name, employee.name) && Objects
          .equals(address, employee.address);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, address);
    }
  }

  public static class Address {
    public String address;
    public int zipCode;

    public Address(String address, int zipCode) {
      this.address = address;
      this.zipCode = zipCode;
    }

    public String getAddress() {
      return address;
    }

    public void setAddress(String address) {
      this.address = address;
    }

    public int getZipCode() {
      return zipCode;
    }

    public void setZipCode(int zipCode) {
      this.zipCode = zipCode;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Address)) {
        return false;
      }
      Address address1 = (Address) o;
      return zipCode == address1.zipCode && Objects.equals(address, address1.address);
    }

    @Override
    public int hashCode() {
      return Objects.hash(address, zipCode);
    }
  }
}
