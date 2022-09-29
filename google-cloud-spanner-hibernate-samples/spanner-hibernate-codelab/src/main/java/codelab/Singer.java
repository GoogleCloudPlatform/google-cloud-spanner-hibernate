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

package codelab;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Date;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;

/**
 * An entity representing a singer.
 */
@Entity
public class Singer {

  @Id
  @GeneratedValue
  @JdbcTypeCode(java.sql.Types.VARCHAR)
  private UUID singerId;

  private String firstName;

  private String lastName;

  @Temporal(TemporalType.DATE)
  private Date birthDate;

  public Singer() {
  }

  /**
   * Alternative constructor.
   */
  public Singer(String firstName, String lastName, Date birthDate) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.birthDate = birthDate;
  }

  public UUID getSingerId() {
    return singerId;
  }

  public void setSingerId(UUID singerId) {
    this.singerId = singerId;
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

  public Date getBirthDate() {
    return birthDate;
  }

  public void setBirthDate(Date birthDate) {
    this.birthDate = birthDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Singer)) {
      return false;
    }

    Singer singer = (Singer) o;

    if (!firstName.equals(singer.firstName)) {
      return false;
    }
    if (!lastName.equals(singer.lastName)) {
      return false;
    }
    return birthDate.equals(singer.birthDate);
  }

  @Override
  public int hashCode() {
    int result = firstName.hashCode();
    result = 31 * result + lastName.hashCode();
    result = 31 * result + birthDate.hashCode();
    return result;
  }
}
