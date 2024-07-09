/*
 * Copyright 2019-2024 Google LLC
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

package com.google.cloud.spanner.hibernate.it.model;

import com.google.cloud.spanner.hibernate.types.SpannerBoolArray;
import com.google.cloud.spanner.hibernate.types.SpannerBytesArray;
import com.google.cloud.spanner.hibernate.types.SpannerDateArray;
import com.google.cloud.spanner.hibernate.types.SpannerFloat32Array;
import com.google.cloud.spanner.hibernate.types.SpannerFloat64Array;
import com.google.cloud.spanner.hibernate.types.SpannerInt64Array;
import com.google.cloud.spanner.hibernate.types.SpannerJsonArray;
import com.google.cloud.spanner.hibernate.types.SpannerNumericArray;
import com.google.cloud.spanner.hibernate.types.SpannerStringArray;
import com.google.cloud.spanner.hibernate.types.SpannerTimestampArray;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.annotations.Type;

/** Test entity for mapping all supported types. */
@Entity
public class AllTypes extends AbstractBaseEntity {

  @Id private Long id;

  private Boolean colBool;

  private byte[] colBytes;

  private LocalDate colDate;

  private Double colFloat64;

  private Float colFloat32;

  private Long colInt64;

  private String colJson;

  private BigDecimal colNumeric;

  private String colString;

  private Instant colTimestamp;

  @Type(SpannerBoolArray.class)
  private List<Boolean> colBoolArray;

  @Type(SpannerBytesArray.class)
  private List<byte[]> colBytesArray;

  @Type(SpannerDateArray.class)
  private List<LocalDate> colDateArray;

  @Type(SpannerFloat64Array.class)
  private List<Double> colFloat64Array;

  @Type(SpannerFloat32Array.class)
  private List<Float> colFloat32Array;

  @Type(SpannerInt64Array.class)
  private List<Long> colInt64Array;

  @Type(SpannerJsonArray.class)
  private List<String> colJsonArray;

  @Type(SpannerNumericArray.class)
  private List<BigDecimal> colNumericArray;

  @Type(SpannerStringArray.class)
  private List<String> colStringArray;

  @Type(SpannerTimestampArray.class)
  private List<Instant> colTimestampArray;

  @Override
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Boolean getColBool() {
    return colBool;
  }

  public void setColBool(Boolean colBool) {
    this.colBool = colBool;
  }

  public byte[] getColBytes() {
    return colBytes;
  }

  public void setColBytes(byte[] colBytes) {
    this.colBytes = colBytes;
  }

  public LocalDate getColDate() {
    return colDate;
  }

  public void setColDate(LocalDate colDate) {
    this.colDate = colDate;
  }

  public Double getColFloat64() {
    return colFloat64;
  }

  public void setColFloat64(Double colFloat64) {
    this.colFloat64 = colFloat64;
  }

  public Float getColFloat32() {
    return colFloat32;
  }

  public void setColFloat32(Float colFloat32) {
    this.colFloat32 = colFloat32;
  }

  public Long getColInt64() {
    return colInt64;
  }

  public void setColInt64(Long colInt64) {
    this.colInt64 = colInt64;
  }

  public String getColJson() {
    return colJson;
  }

  public void setColJson(String colJson) {
    this.colJson = colJson;
  }

  public BigDecimal getColNumeric() {
    return colNumeric;
  }

  public void setColNumeric(BigDecimal colNumeric) {
    this.colNumeric = colNumeric;
  }

  public String getColString() {
    return colString;
  }

  public void setColString(String colString) {
    this.colString = colString;
  }

  public Instant getColTimestamp() {
    return colTimestamp;
  }

  public void setColTimestamp(Instant colTimestamp) {
    this.colTimestamp = colTimestamp;
  }

  public List<Boolean> getColBoolArray() {
    return colBoolArray;
  }

  public void setColBoolArray(List<Boolean> colBoolArray) {
    this.colBoolArray = colBoolArray;
  }

  public List<byte[]> getColBytesArray() {
    return colBytesArray;
  }

  public void setColBytesArray(List<byte[]> colBytesArray) {
    this.colBytesArray = colBytesArray;
  }

  public List<LocalDate> getColDateArray() {
    return colDateArray;
  }

  public void setColDateArray(List<LocalDate> colDateArray) {
    this.colDateArray = colDateArray;
  }

  public List<Double> getColFloat64Array() {
    return colFloat64Array;
  }

  public void setColFloat64Array(List<Double> colFloat64Array) {
    this.colFloat64Array = colFloat64Array;
  }

  public List<Float> getColFloat32Array() {
    return colFloat32Array;
  }

  public void setColFloat32Array(List<Float> colFloat32Array) {
    this.colFloat32Array = colFloat32Array;
  }

  public List<Long> getColInt64Array() {
    return colInt64Array;
  }

  public void setColInt64Array(List<Long> colInt64Array) {
    this.colInt64Array = colInt64Array;
  }

  public List<String> getColJsonArray() {
    return colJsonArray;
  }

  public void setColJsonArray(List<String> colJsonArray) {
    this.colJsonArray = colJsonArray;
  }

  public List<BigDecimal> getColNumericArray() {
    return colNumericArray;
  }

  public void setColNumericArray(List<BigDecimal> colNumericArray) {
    this.colNumericArray = colNumericArray;
  }

  public List<String> getColStringArray() {
    return colStringArray;
  }

  public void setColStringArray(List<String> colStringArray) {
    this.colStringArray = colStringArray;
  }

  public List<Instant> getColTimestampArray() {
    return colTimestampArray;
  }

  public void setColTimestampArray(List<Instant> colTimestampArray) {
    this.colTimestampArray = colTimestampArray;
  }
}
