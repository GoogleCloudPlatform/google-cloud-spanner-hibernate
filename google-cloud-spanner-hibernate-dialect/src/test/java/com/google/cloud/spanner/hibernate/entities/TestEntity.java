/*
 * Copyright 2019-2020 Google LLC
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

import java.io.Serializable;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * A test entity class used for generating schema statements.
 *
 * @author Chengyuan Zhao
 */
@Entity
@Table(name = "`test_table`")
public class TestEntity {

  @EmbeddedId
  public IdClass id;

  @Column(nullable = true)
  public String stringVal;

  @Column(name = "`boolColumn`")
  public boolean boolVal;

  public long longVal;

  @ElementCollection
  List<String> stringList;

  @Embeddable
  public static class IdClass implements Serializable {

    @Column(name = "`ID1`")
    public long id1;

    public String id2;
  }
}
