package com.google.cloud.spanner.hibernate.entities;

import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import org.hibernate.annotations.Type;

/**
 * An entity which uses a {@link OneToMany}, which triggers usage of the Hibernate
 * unique constraint.
 */
@Entity
public class Airport {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Type(type = "uuid-char")
  private UUID id;

  @OneToMany
  private List<Airplane> airplanes;
}
