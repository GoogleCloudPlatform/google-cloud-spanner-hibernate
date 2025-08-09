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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.Type;

@Entity
@Table(indexes = {@Index(name = "idx_singer_active", columnList = "active")})
@DynamicInsert
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

  @ColumnDefault("true")
  private Boolean active;

  @OneToMany(mappedBy = "singer")
  @BatchSize(size = 10)
  private List<Album> albums;

  @OneToMany(mappedBy = "singer")
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

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
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
