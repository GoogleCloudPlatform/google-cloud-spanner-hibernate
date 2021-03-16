package org.hibernate.dialect.entities;

import com.google.cloud.spanner.hibernate.types.SpannerArrayListType;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

/**
 * A test entity with a lot of Array column fields.
 */
@TypeDefs({
    @TypeDef(
        name = "spanner-array",
        typeClass = SpannerArrayListType.class
    )
})
@Entity
public class ListEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Type(type = "uuid-char")
  private UUID id;

  @Type(type = "spanner-array")
  private List<Boolean> booleanList;

  @Type(type = "spanner-array")
  private List<byte[]> byteList;

  @Type(type = "spanner-array")
  private List<Timestamp> timestampList;

  @Type(type = "spanner-array")
  private List<Double> doubleList;

  @Type(type = "spanner-array")
  private List<Integer> intList;

  @Type(type = "spanner-array")
  private List<BigDecimal> bigDecimalList;

  @Type(type = "spanner-array")
  private List<String> stringList;

  public List<Boolean> getBooleanList() {
    return booleanList;
  }

  public void setBooleanList(List<Boolean> booleanList) {
    this.booleanList = booleanList;
  }

  public List<byte[]> getByteList() {
    return byteList;
  }

  public void setByteList(List<byte[]> byteList) {
    this.byteList = byteList;
  }

  public List<Timestamp> getTimestampList() {
    return timestampList;
  }

  public void setTimestampList(List<Timestamp> timestampList) {
    this.timestampList = timestampList;
  }

  public List<Double> getDoubleList() {
    return doubleList;
  }

  public void setDoubleList(List<Double> doubleList) {
    this.doubleList = doubleList;
  }

  public List<Integer> getIntList() {
    return intList;
  }

  public void setIntList(List<Integer> intList) {
    this.intList = intList;
  }

  public List<BigDecimal> getBigDecimalList() {
    return bigDecimalList;
  }

  public void setBigDecimalList(List<BigDecimal> bigDecimalList) {
    this.bigDecimalList = bigDecimalList;
  }

  public List<String> getStringList() {
    return stringList;
  }

  public void setStringList(List<String> stringList) {
    this.stringList = stringList;
  }
}
