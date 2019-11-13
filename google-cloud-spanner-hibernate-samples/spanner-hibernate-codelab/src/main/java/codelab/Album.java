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

package codelab;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import org.hibernate.annotations.Type;

/**
 * An entity representing a musical album.
 */
@Entity
public class Album {

  @Id
  @GeneratedValue
  @Type(type = "uuid-char")
  UUID albumId;

  @ManyToOne
  Singer singer;

  String albumTitle;

  public Album() {
  }

  public Album(Singer singer, String albumTitle) {
    this.singer = singer;
    this.albumTitle = albumTitle;
  }

  public UUID getAlbumId() {
    return albumId;
  }

  public void setAlbumId(UUID albumId) {
    this.albumId = albumId;
  }

  public Singer getSinger() {
    return singer;
  }

  public void setSinger(Singer singer) {
    this.singer = singer;
  }

  public String getAlbumTitle() {
    return albumTitle;
  }

  public void setAlbumTitle(String albumTitle) {
    this.albumTitle = albumTitle;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Album)) {
      return false;
    }

    Album album = (Album) o;

    if (!singer.equals(album.singer)) {
      return false;
    }
    return albumTitle.equals(album.albumTitle);
  }

  @Override
  public int hashCode() {
    int result = singer.hashCode();
    result = 31 * result + albumTitle.hashCode();
    return result;
  }
}
