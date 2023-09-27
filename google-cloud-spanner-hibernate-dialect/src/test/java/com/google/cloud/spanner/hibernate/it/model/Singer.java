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

package com.google.cloud.spanner.hibernate.it.model;

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

@Entity
@Check(constraints = "first_name is not null or last_name is not null")
public class Singer extends AbstractBaseEntity {

  @Column(length = 200)
  private String firstName;

  @Column(length = 200)
  private String lastName;

  @Generated(GenerationTime.ALWAYS)
  @Column(length = 400, insertable = false, updatable = false,
      columnDefinition = "string(400) as (\n"
          + "case\n"
          + "  when firstName is null then lastName\n"
          + "  when lastName is null then firstName\n"
          + "  else firstName || ' ' || lastName\n"
          + "end) stored")
  private String fullName;

  private LocalDate birthDate;

  protected Singer() {
  }

  public Singer(String firstName, String lastName) {
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public LocalDate getBirthDate() {
    return birthDate;
  }

  public void setBirthDate(LocalDate birthDate) {
    this.birthDate = birthDate;
  }
}
