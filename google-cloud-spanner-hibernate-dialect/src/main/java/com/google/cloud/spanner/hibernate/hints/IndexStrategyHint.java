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

import com.google.cloud.spanner.hibernate.hints.Hints.IndexStrategy;
import com.google.common.collect.ImmutableList;

class IndexStrategyHint extends ReplaceQueryPartsHint {

  private static final String HINT = "INDEX_STRATEGY";

  static IndexStrategyHint from(String table, IndexStrategy indexStrategy,
      ReplaceMode replaceMode) {
    return new IndexStrategyHint(ImmutableList.of(
        new Replacement(Hints.from(table), indexStrategyFrom(table, indexStrategy), replaceMode)));
  }

  static IndexStrategyHint join(String table, IndexStrategy indexStrategy,
      ReplaceMode replaceMode) {
    return new IndexStrategyHint(ImmutableList.of(
        new Replacement(Hints.join(table), indexStrategyJoin(table, indexStrategy), replaceMode)));
  }

  private IndexStrategyHint(ImmutableList<Replacement> replacements) {
    super(replacements);
  }

  private static String indexStrategyFrom(String table, IndexStrategy indexStrategy) {
    return fromTableHint(table, HINT, indexStrategy.name());
  }

  private static String indexStrategyJoin(String table, IndexStrategy indexStrategy) {
    return joinTableHint(table, HINT, indexStrategy.name());
  }

}
