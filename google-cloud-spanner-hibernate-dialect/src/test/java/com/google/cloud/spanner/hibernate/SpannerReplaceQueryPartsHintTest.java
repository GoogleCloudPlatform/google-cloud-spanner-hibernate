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

package com.google.cloud.spanner.hibernate;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SpannerReplaceQueryPartsHintTest {

  @Test
  public void testToComment() {
    assertEquals("{\n"
        + "  \"spanner_replacements\": {\n"
        + "    \"from\": \"to\"\n"
        + "  }\n"
        + "}", SpannerReplaceQueryPartsHint.of("from", "to").toComment());
    assertEquals("{\n"
        + "  \"spanner_replacements\": {\n"
        + "    \"from1\": \"to1\",\n"
        + "    \"from2\": \"to2\"\n"
        + "  }\n"
        + "}", SpannerReplaceQueryPartsHint.of("from1", "to1", "from2", "to2").toComment());
  }

  @Test
  public void testRoundTrip() {
    assertRoundTrip(SpannerReplaceQueryPartsHint.of("from", "to"));
    assertRoundTrip(SpannerReplaceQueryPartsHint.of("from1", "to1", "from2", "to2"));
    assertRoundTrip(SpannerReplaceQueryPartsHint.of(
        "@foo", "@bar",
        "123", "321",
        "-", "+"));
    assertRoundTrip(SpannerReplaceQueryPartsHint.of("select *\nfrom foo\nwhere true", "select *\nfrom bar\nwhere false"));
  }

  @Test
  public void testForceIndex() {
    assertEquals("{\n"
        + "  \"spanner_replacements\": {\n"
        + "    \" from singers \": \" from singers@{FORCE_INDEX=idx_singers_active} \",\n"
        + "    \" join singers \": \" join singers@{FORCE_INDEX=idx_singers_active} \"\n"
        + "  }\n"
        + "}", Hints.forceIndex("singers", "idx_singers_active").toComment());
  }

  private void assertRoundTrip(SpannerReplaceQueryPartsHint hint) {
    assertEquals(hint, SpannerReplaceQueryPartsHint.fromComment(hint.toComment()));
  }

}
