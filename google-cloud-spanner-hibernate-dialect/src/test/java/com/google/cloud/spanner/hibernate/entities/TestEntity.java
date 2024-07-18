/*
 * Copyright 2019-2023 Google LLC
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

package com.google.cloud.spanner.hibernate.entities;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.sql.Types;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;

/**
 * A test entity class used for generating schema statements.
 *
 * @author Chengyuan Zhao
 */
@Entity
@Table(name = "`test_table`")
public class TestEntity {

  @EmbeddedId public IdClass id;

  @Column(nullable = true)
  public String stringVal;

  @Column(name = "`boolColumn`")
  public boolean boolVal;

  public long longVal;

  // Types.REAL is the JDBC equivalent of a single-precision floating point number.
  // This is translated to float32 in Spanner.
  @JdbcTypeCode(Types.REAL)
  public float floatVal;

  // Hibernate translates java.lang.float to Types.FLOAT.
  // According to the JDBC spec, Types.FLOAT == Types.DOUBLE.
  // That means that this column is stored as a float64 in Spanner.
  public float floatValStoredAsDouble;

  @ElementCollection List<String> stringList;

  /** A simple Hibernate embedded ID used in tests. */
  @Embeddable
  public static class IdClass implements Serializable {

    @Column(name = "`ID1`")
    public long id1;

    public String id2;
  }
}
