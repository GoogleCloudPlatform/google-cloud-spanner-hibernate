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


import com.google.cloud.spanner.hibernate.Interleaved;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;
import java.util.Objects;

/**
 * Track is interleaved in Album.
 */
@Entity
@Interleaved(parentEntity = Album.class, cascadeDelete = true)
public class Track extends AbstractBaseEntity {

  /**
   * Track is interleaved in the Album entity. This requires the primary key of Track to include all
   * the columns of the primary key of Album, in addition to its own primary key value.
   * {@link TrackId} defines the composite primary key of the {@link Track} entity.
   */
  @Embeddable
  public static class TrackId implements Serializable {

    /**
     * `id` is the primary key column that Track 'inherits' from Album.
     */
    private Long id;

    /**
     * `trackNumber` is the additional primary key column that is used by Track.
     */
    private long trackNumber;

    protected TrackId() {
    }

    public TrackId(Long id, long trackNumber) {
      this.id = id;
      this.trackNumber = trackNumber;
    }

    public Long getId() {
      return id;
    }

    public long getTrackNumber() {
      return trackNumber;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, trackNumber);
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof TrackId)) {
        return false;
      }
      TrackId other = (TrackId) o;
      return Objects.equals(id, other.id) && Objects.equals(trackNumber, other.trackNumber);
    }
  }

  /**
   * Hibernate requires a default constructor.
   */
  protected Track() {
  }

  public Track(Album album, long trackNumber, String title) {
    setTrackId(new TrackId(album.getId(), trackNumber));
    this.title = title;
  }

  /**
   * Use the @EmbeddedId annotation to define a composite primary key from an @Embeddable class.
   */
  @EmbeddedId
  private TrackId trackId;

  /**
   * The "id" column is both part of the primary key, and a reference to the albums table.
   */
  @ManyToOne(optional = false)
  @JoinColumn(name = "id", updatable = false, insertable = false)
  private Album album;

  @Column(length = 100, nullable = false)
  private String title;

  private Double sampleRate;

  @Override
  public TrackId getId() {
    return trackId;
  }

  public TrackId getTrackId() {
    return trackId;
  }

  public void setTrackId(TrackId trackId) {
    this.trackId = trackId;
  }

  public Album getAlbum() {
    return album;
  }

  public void setAlbum(Album album) {
    this.album = album;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Double getSampleRate() {
    return sampleRate;
  }

  public void setSampleRate(Double sampleRate) {
    this.sampleRate = sampleRate;
  }
}
