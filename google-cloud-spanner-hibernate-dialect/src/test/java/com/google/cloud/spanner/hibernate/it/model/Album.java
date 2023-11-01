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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

/**
 * Album entity definition.
 */
@Entity
public class Album extends AbstractBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "album_id_generator")
  @GenericGenerator(
      name = "album_id_generator",
      // TODO: Switch to PooledBitReversedSequenceStyleGenerator when that is available and the
      //       emulator supports it.
      strategy = "com.google.cloud.spanner.hibernate.BitReversedSequenceStyleGenerator",
      parameters = {
          // Use a separate name for each entity to ensure that it uses a separate table.
          @Parameter(name = SequenceStyleGenerator.SEQUENCE_PARAM, value = "album_id"),
          @Parameter(name = SequenceStyleGenerator.INCREMENT_PARAM, value = "1000"),
      })
  private Long id;

  @Column(nullable = false, length = 300)
  private String title;

  private BigDecimal marketingBudget;

  private LocalDate releaseDate;

  private byte[] coverPicture;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  private Singer singer;

  /**
   * The 'id' column in Track maps to Album. The corresponding property is called 'album'.
   */
  @OneToMany(mappedBy = "album")
  private List<Track> tracks;

  protected Album() {
  }

  public Album(Singer singer, String title) {
    this.singer = singer;
    this.title = title;
  }

  @Override
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public BigDecimal getMarketingBudget() {
    return marketingBudget;
  }

  public void setMarketingBudget(BigDecimal marketingBudget) {
    this.marketingBudget = marketingBudget;
  }

  public LocalDate getReleaseDate() {
    return releaseDate;
  }

  public void setReleaseDate(LocalDate releaseDate) {
    this.releaseDate = releaseDate;
  }

  public byte[] getCoverPicture() {
    return coverPicture;
  }

  public void setCoverPicture(byte[] coverPicture) {
    this.coverPicture = coverPicture;
  }

  public Singer getSinger() {
    return singer;
  }

  public void setSinger(Singer singer) {
    this.singer = singer;
  }

  public List<Track> getTracks() {
    return tracks;
  }

  public void setTracks(List<Track> tracks) {
    this.tracks = tracks;
  }
}
