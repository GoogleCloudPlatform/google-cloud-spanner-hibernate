/*
 * Copyright 2019-2025 Google LLC
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

package com.google.cloud.spanner.hibernate;

import org.hibernate.MappingException;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

/** Identity column support for Spanner using auto_increment and 'then return'. */
public class SpannerIdentityColumnSupport extends IdentityColumnSupportImpl {
  public static final SpannerIdentityColumnSupport INSTANCE = new SpannerIdentityColumnSupport();

  private SpannerIdentityColumnSupport() {}

  @Override
  public boolean supportsIdentityColumns() {
    return true;
  }

  @Override
  public String appendIdentitySelectToInsert(String identityColumnName, String insertString) {
    return insertString + " THEN RETURN " + identityColumnName;
  }

  @Override
  public String getIdentitySelectString(String table, String column, int type)
      throws MappingException {
    throw new MappingException(
        getClass().getName() + " does not support selecting the last generated identity value");
  }

  @Override
  public String getIdentityColumnString(int type) {
    return "GENERATED BY DEFAULT AS IDENTITY (BIT_REVERSED_POSITIVE)";
  }
}
