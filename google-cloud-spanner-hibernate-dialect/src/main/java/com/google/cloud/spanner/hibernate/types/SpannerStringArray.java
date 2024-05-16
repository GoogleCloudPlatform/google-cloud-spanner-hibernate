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

package com.google.cloud.spanner.hibernate.types;

import com.google.cloud.spanner.Type.Code;
import java.util.List;

/**
 * Hibernate type definition for
 *
 * <pre>{@code ARRAY<STRING>}</pre>
 *
 * .
 */
public class SpannerStringArray extends AbstractSpannerArrayType<String, String> {

  @Override
  public Code getSpannerTypeCode() {
    return Code.STRING;
  }

  @Override
  public String[] toArray(List<String> value) {
    return value == null ? null : value.toArray(new String[0]);
  }
}
