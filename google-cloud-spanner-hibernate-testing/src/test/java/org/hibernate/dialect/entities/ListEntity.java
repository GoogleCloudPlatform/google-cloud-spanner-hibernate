/*
 * Copyright 2023 Google LLC
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

package org.hibernate.dialect.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;

/**
 * A test entity with a lot of Array column fields.
 */
// TODO: Re-implement array types
//@TypeDefs({
//    @TypeDef(
//        name = "spanner-array",
//        typeClass = SpannerArrayListType.class
//    )
//})
@Entity
public class ListEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @JdbcTypeCode(Types.CHAR)
  private UUID id;

  @Transient
  //@Type(type = "spanner-array")
  private List<Boolean> booleanList;

  @Transient
  //@Type(type = "spanner-array")
  private List<byte[]> byteList;

  @Transient
  //@Type(type = "spanner-array")
  private List<Timestamp> timestampList;

  @Transient
  //@Type(type = "spanner-array")
  private List<Double> doubleList;

  @Transient
  //@Type(type = "spanner-array")
  private List<Integer> intList;

  @Transient
  //@Type(type = "spanner-array")
  private List<BigDecimal> bigDecimalList;

  @Transient
  //@Type(type = "spanner-array")
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
