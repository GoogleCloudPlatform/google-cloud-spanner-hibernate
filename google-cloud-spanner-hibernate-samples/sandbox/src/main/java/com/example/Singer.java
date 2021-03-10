package com.example;

import com.google.cloud.spanner.hibernate.types.SpannerArrayListType;
import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@TypeDef(
    name = "list-array",
    typeClass = SpannerArrayListType.class
)
@Entity
public class Singer {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Type(type = "uuid-char")
  private UUID id;

  @Type(type = "list-array")
  public List<Integer> numbers;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public List<Integer> getNumbers() {
    return numbers;
  }

  public void setNumbers(List<Integer> numbers) {
    this.numbers = numbers;
  }

  @Override
  public String toString() {
    return "Singer{" +
        "id=" + id +
        ", numbers=" + numbers +
        '}';
  }
}
