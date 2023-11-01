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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.List;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

/**
 * Venue entity.
 */
@Entity
public class Venue extends AbstractBaseEntity {

  /**
   * {@link VenueDescription} is a POJO that is used for the JSON field 'description' of the
   * {@link Venue} entity. It is automatically serialized and deserialized when an instance of the
   * entity is loaded or persisted.
   */
  public static class VenueDescription implements Serializable {

    private int capacity;
    private String type;
    private String location;

    public int getCapacity() {
      return capacity;
    }

    public void setCapacity(int capacity) {
      this.capacity = capacity;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getLocation() {
      return location;
    }

    public void setLocation(String location) {
      this.location = location;
    }
  }

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "venue_id_generator")
  @GenericGenerator(
      name = "venue_id_generator",
      // TODO: Switch to PooledBitReversedSequenceStyleGenerator when that is available and the
      //       emulator supports it.
      strategy = "com.google.cloud.spanner.hibernate.BitReversedSequenceStyleGenerator",
      parameters = {
          // Use a separate name for each entity to ensure that it uses a separate table.
          @Parameter(name = SequenceStyleGenerator.SEQUENCE_PARAM, value = "venue_id"),
          @Parameter(name = SequenceStyleGenerator.INCREMENT_PARAM, value = "1000"),
      })
  private Long id;

  private String name;

  /**
   * This field maps to a JSON column in the database. The value is automatically
   * serialized/deserialized to a {@link VenueDescription} instance.
   */
  // TODO: Make this non-transient when we support Hibernate 6.
  @Transient
  private VenueDescription description;

  @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL)
  private List<Concert> concerts;

  protected Venue() {
  }

  public Venue(String name, VenueDescription description) {
    this.name = name;
    this.description = description;
  }

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

  public VenueDescription getDescription() {
    return description;
  }

  public void setDescription(VenueDescription description) {
    this.description = description;
  }

  public List<Concert> getConcerts() {
    return concerts;
  }

  public void setConcerts(List<Concert> concerts) {
    this.concerts = concerts;
  }
}
