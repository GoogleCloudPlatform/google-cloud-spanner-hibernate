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

import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

/**
 * Concert entity.
 */
@Entity
// NOTE: Hibernate 5 does not generate a DDL statement for this constraint.
@Check(constraints = "endTime > startTime")
public class Concert extends AbstractBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "concert_id_generator")
  @GenericGenerator(
      name = "concert_id_generator",
      // TODO: Switch to PooledBitReversedSequenceStyleGenerator when that is available and the
      //       emulator supports it.
      strategy = "com.google.cloud.spanner.hibernate.BitReversedSequenceStyleGenerator",
      parameters = {
          // Use a separate name for each entity to ensure that it uses a separate table.
          @Parameter(name = SequenceStyleGenerator.SEQUENCE_PARAM, value = "concert_id"),
          @Parameter(name = SequenceStyleGenerator.INCREMENT_PARAM, value = "1000"),
      })
  private Long id;

  @Column(nullable = false)
  private String name;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private Venue venue;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private Singer singer;

  @Column(nullable = false)
  private OffsetDateTime startTime;

  @Column(nullable = false)
  private OffsetDateTime endTime;

  protected Concert() {
  }

  public Concert(Venue venue, Singer singer) {
    this(venue, singer, null, null, null);
  }

  /** Constructor. */
  public Concert(Venue venue, Singer singer, String name, OffsetDateTime startTime,
      OffsetDateTime endTime) {
    this.venue = venue;
    this.singer = singer;
    this.name = name;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  @Override
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Venue getVenue() {
    return venue;
  }

  public void setVenue(Venue venue) {
    this.venue = venue;
  }

  public Singer getSinger() {
    return singer;
  }

  public void setSinger(Singer singer) {
    this.singer = singer;
  }

  public OffsetDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(OffsetDateTime startTime) {
    this.startTime = startTime;
  }

  public OffsetDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(OffsetDateTime endTime) {
    this.endTime = endTime;
  }
}
