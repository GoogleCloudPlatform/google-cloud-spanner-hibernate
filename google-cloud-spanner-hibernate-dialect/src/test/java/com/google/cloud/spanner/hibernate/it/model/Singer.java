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

import com.google.cloud.spanner.hibernate.annotations.PooledBitReversedSequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/** Singer entity definition. */
@Entity
// NOTE: Hibernate 5 does not generate a DDL statement for this constraint.
@Check(constraints = "first_name is not null or last_name is not null")
@Table(indexes = {@Index(name = "idx_singer_active", columnList = "active")})
public class Singer extends AbstractBaseEntity {

  @Id
  @PooledBitReversedSequenceGenerator(sequenceName = "singer_id_sequence")
  private Long id;

  @Column(length = 200)
  private String firstName;

  @Column(length = 200)
  private String lastName;

  @Generated(event = {EventType.INSERT, EventType.UPDATE})
  @Column(
      length = 400,
      insertable = false,
      updatable = false,
      columnDefinition =
          "string(400) as (\n"
              + "case\n"
              + "  when firstName is null then lastName\n"
              + "  when lastName is null then firstName\n"
              + "  else firstName || ' ' || lastName\n"
              + "end) stored")
  private String fullName;

  private LocalDate birthDate;

  @ColumnDefault("true")
  private boolean active;

  @OneToMany(mappedBy = "singer")
  private List<Album> albums;

  protected Singer() {}

  /** Constructor for a new active Singer. */
  public Singer(String firstName, String lastName) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.active = true;
  }

  @Override
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public List<Album> getAlbums() {
    return albums;
  }

  public void setAlbums(List<Album> albums) {
    this.albums = albums;
  }
}
