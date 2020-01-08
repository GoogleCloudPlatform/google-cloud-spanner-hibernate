package org.hibernate.dialect.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class GrandParent {

  @Id
  @GeneratedValue
  public long grandParentId;

  public String name;

  public GrandParent() {
  }

  public long getGrandParentId() {
    return grandParentId;
  }

  public void setGrandParentId(long grandParentId) {
    this.grandParentId = grandParentId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "GrandParent{" +
        "grandParentId=" + grandParentId +
        ", name='" + name + '\'' +
        '}';
  }
}
