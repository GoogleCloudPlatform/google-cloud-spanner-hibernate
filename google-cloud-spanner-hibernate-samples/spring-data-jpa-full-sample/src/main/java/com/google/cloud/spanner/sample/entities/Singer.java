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

package com.google.cloud.spanner.sample.entities;

import com.google.cloud.spanner.hibernate.types.SpannerStringArray;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import org.hibernate.annotations.Type;

@Entity
@Table(indexes = {@Index(name = "idx_singer_active", columnList = "active")})
public class Singer extends AbstractNonInterleavedEntity {

  @Column(length = 100)
  private String firstName;

  @Column(length = 200)
  private String lastName;

  @Column(
      columnDefinition =
          "STRING(300) AS (\n"
              + "CASE WHEN first_name IS NULL THEN last_name\n"
              + "     WHEN last_name IS NULL THEN first_name\n"
              + "     ELSE first_name || ' ' || last_name\n"
              + "END) STORED",
      insertable = false,
      updatable = false)
  private String fullName;

  @Column(columnDefinition = "ARRAY<STRING(MAX)>")
  @Type(SpannerStringArray.class)
  private List<String> nickNames;

  private boolean active;

  @OneToMany
  @JoinColumn(name = "singer_id")
  private List<Album> albums;

  @OneToMany
  @JoinColumn(name = "singer_id")
  private List<Concert> concerts;

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

  public List<String> getNickNames() {
    return nickNames;
  }

  public void setNickNames(List<String> nickNames) {
    this.nickNames = nickNames;
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

  public List<Concert> getConcerts() {
    return concerts;
  }

  public void setConcerts(List<Concert> concerts) {
    this.concerts = concerts;
  }
}
