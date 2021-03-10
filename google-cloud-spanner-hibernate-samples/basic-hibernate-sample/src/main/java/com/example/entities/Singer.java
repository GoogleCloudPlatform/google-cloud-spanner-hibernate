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

package com.example.entities;

import com.google.cloud.spanner.hibernate.types.SpannerArrayListType;
import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@TypeDefs({
  @TypeDef(
    name = "spanner-array",
    typeClass = SpannerArrayListType.class
  )
})
@Entity
public class Singer {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Type(type = "uuid-char")
  private UUID singerId;

  private String name;

  @OneToMany(mappedBy = "singer")
  List<Album> albums;

  @Type(type = "spanner-array")
  private List<String> nickNames;

  public Singer(String name, List<Album> albums, List<String> nickNames) {
    this.name = name;
    this.albums = albums;
    this.nickNames = nickNames;
  }

  // Default constructor used by JPA
  public Singer() {

  }

  public UUID getSingerId() {
    return singerId;
  }

  public void setSingerId(UUID singerId) {
    this.singerId = singerId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void addAlbum(Album album) {
    this.albums.add(album);
  }

  public List<Album> getAlbums() {
    return albums;
  }

  public void setAlbums(List<Album> albums) {
    this.albums = albums;
  }

  public List<String> getNickNames() {
    return nickNames;
  }

  public void setNickNames(List<String> nickNames) {
    this.nickNames = nickNames;
  }

  @Override
  public String toString() {
    return "Singer{" +
        "singerId=" + singerId
        + ", name='" + name + '\''
        + ", albums=" + albums
        + ", nickNames=" + nickNames
        + '}';
  }
}
