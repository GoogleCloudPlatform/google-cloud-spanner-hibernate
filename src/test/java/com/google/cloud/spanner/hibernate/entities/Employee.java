package com.google.cloud.spanner.hibernate.entities;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * A test entity that uses features in Hibernate that are unsupported in Spanner.
 *
 * @author Daniel Zou
 */
@Entity
public class Employee {

  @Id
  public Long id;

  @ManyToOne(cascade = {CascadeType.ALL})
  public Employee manager;

  @Column(unique = true)
  public String name;
}
