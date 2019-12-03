package com.google.cloud.spanner.hibernate.entities;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import org.hibernate.annotations.Type;

/**
 * A sample Hibernate entity which helps verify usage of the unique constraint in Hibernate.
 */
@Entity
public class Airplane {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Type(type = "uuid-char")
  private UUID id;

  @Column(unique = true)
  private String modelName;
}
