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

import com.google.cloud.spanner.hibernate.Interleaved;
import com.google.cloud.spanner.sample.entities.Track.TrackId;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.sql.Types;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.domain.Persistable;

/** Track is interleaved in Album, and therefore uses a composite primary key. */
@Interleaved(parentEntity = Album.class, cascadeDelete = true)
@Entity
public class Track extends AbstractEntity implements Persistable<TrackId> {

  /**
   * Track is interleaved in the Album entity. This requires the primary key of Track to include all
   * the columns of the primary key of Album, in addition to its own primary key value. {@link
   * TrackId} defines the composite primary key of the {@link Track} entity.
   */
  @Embeddable
  public static class TrackId implements Serializable {

    /** `id` is the primary key column that Track 'inherits' from Album. */
    @JdbcTypeCode(Types.CHAR)
    private UUID id;

    /** `trackNumber` is the additional primary key column that is used by Track. */
    private long trackNumber;

    protected TrackId() {}

    public TrackId(UUID id, long trackNumber) {
      this.id = id;
      this.trackNumber = trackNumber;
    }

    public UUID getId() {
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

  /** Factory method for creating a new {@link Track} belonging to an {@link Album}. */
  public static Track createNew(Album album, long trackNumber) {
    return new Track(album, trackNumber, true);
  }

  /** Hibernate requires a default constructor. */
  protected Track() {}

  private Track(Album album, long trackNumber, boolean newRecord) {
    setTrackId(new TrackId(album.getId(), trackNumber));
    this.newRecord = newRecord;
  }

  /** Use the @EmbeddedId annotation to define a composite primary key from an @Embeddable class. */
  @EmbeddedId private TrackId trackId;

  /** The "id" column is both part of the primary key, and a reference to the albums table. */
  @ManyToOne(optional = false)
  @JoinColumn(name = "id", updatable = false, insertable = false)
  private Album album;

  @Basic(optional = false)
  @Column(length = 100)
  private String title;

  private Double sampleRate;

  /**
   * This field is only used to track whether the entity has been persisted or not. This prevents
   * Hibernate from doing a round-trip to the database to check whether the Track exists every time
   * we call save(Track). The reason that we need this for this entity is that we manually assign
   * the primary key value to {@link Track}. That again means that Hibernate cannot determine
   * whether an instance of {@link Track} has already been persisted or not based on the existence
   * of a primary key value.
   */
  @Transient private boolean newRecord;

  @Override
  public TrackId getId() {
    return trackId;
  }

  @Override
  public boolean isNew() {
    return newRecord;
  }

  /** This method resets the 'newRecord' field after it has been persisted to the database. */
  @PostPersist
  public void resetPersisted() {
    newRecord = false;
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
