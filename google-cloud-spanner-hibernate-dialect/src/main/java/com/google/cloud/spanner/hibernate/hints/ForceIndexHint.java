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

package com.google.cloud.spanner.hibernate.hints;

import static com.google.cloud.spanner.hibernate.hints.Hints.fromTableHint;
import static com.google.cloud.spanner.hibernate.hints.Hints.joinTableHint;

import com.google.common.collect.ImmutableList;

class ForceIndexHint extends ReplaceQueryPartsHint {

  private static final String HINT = "FORCE_INDEX";

  static ForceIndexHint from(String table, String index, ReplaceMode replaceMode) {
    return new ForceIndexHint(
        ImmutableList.of(
            new Replacement(Hints.from(table), forceIndexFrom(table, index), replaceMode)));
  }

  static ForceIndexHint join(String table, String index, ReplaceMode replaceMode) {
    return new ForceIndexHint(
        ImmutableList.of(
            new Replacement(Hints.join(table), forceIndexJoin(table, index), replaceMode)));
  }

  private ForceIndexHint(ImmutableList<Replacement> replacements) {
    super(replacements);
  }

  private static String forceIndexFrom(String table, String index) {
    return fromTableHint(table, HINT, index);
  }

  private static String forceIndexJoin(String table, String index) {
    return joinTableHint(table, HINT, index);
  }
}
