package org.hibernate.dialect.entities;

import com.google.cloud.spanner.hibernate.Interleaved;
import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Entity
@Interleaved(parentEntity = GrandParent.class)
public class Parent {

  @EmbeddedId
  public ParentId parentId;

  public String name;

  public void setParentId(ParentId parentId) {
    this.parentId = parentId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ParentId getParentId() {
    return parentId;
  }

  public String getName() {
    return name;
  }

  @Embeddable
  public static class ParentId implements Serializable {

    public long grandParentId;

    public long parentId;

    public ParentId(long grandParentId, long parentId) {
      this.grandParentId = grandParentId;
      this.parentId = parentId;
    }

    public ParentId() {
    }

    @Override
    public String toString() {
      return "ParentId{" +
          "grandParentId=" + grandParentId +
          ", parentId=" + parentId +
          '}';
    }
  }

  @Override
  public String toString() {
    return "Parent{" +
        "parentId=" + parentId +
        ", name='" + name + '\'' +
        '}';
  }
}
