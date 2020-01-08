package org.hibernate.dialect.entities;

import com.google.cloud.spanner.hibernate.Interleaved;
import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import org.hibernate.dialect.entities.Parent.ParentId;

@Entity
@Interleaved(parentEntity = Parent.class)
public class Child {

  @EmbeddedId
  public ChildId childId;

  public String name;

  public void setChildId(ChildId childId) {
    this.childId = childId;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Embeddable
  public static class ChildId implements Serializable {

    ParentId parentId;

    public long childId;

    public ChildId(ParentId parentId, long childId) {
      this.parentId = parentId;
      this.childId = childId;
    }

    public ChildId() {
    }

    @Override
    public String toString() {
      return "ChildId{" +
          "parentId=" + parentId +
          ", childId=" + childId +
          '}';
    }
  }

  @Override
  public String toString() {
    return "Child{" +
        "childId=" + childId +
        ", name='" + name + '\'' +
        '}';
  }
}
