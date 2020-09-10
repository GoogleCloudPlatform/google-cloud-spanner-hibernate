package com.google.cloud.spanner.hibernate.entities;

import java.math.BigDecimal;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Account {

  @Id
  private long id;

  private String name;

  private BigDecimal amount;

  // Default constructor for Hibernate
  Account() {}

  public Account(long id, String name, BigDecimal amount) {
    this.id = id;
    this.name = name;
    this.amount = amount;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
}
