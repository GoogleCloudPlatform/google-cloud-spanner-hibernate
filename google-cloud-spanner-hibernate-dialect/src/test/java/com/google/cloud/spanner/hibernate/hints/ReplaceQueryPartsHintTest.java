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

import static org.junit.Assert.assertEquals;

import com.google.cloud.spanner.hibernate.hints.ReplaceQueryPartsHint.ReplaceMode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for the {@link ReplaceQueryPartsHint} util class. */
@RunWith(JUnit4.class)
public class ReplaceQueryPartsHintTest {

  @Test
  public void testToComment() {
    assertEquals(
        "{\n"
            + "  \"spanner_replacements\": [\n"
            + "    {\n"
            + "      \"regex\": \"from\",\n"
            + "      \"replacement\": \"to\",\n"
            + "      \"replace_mode\": \"ALL\"\n"
            + "    }\n"
            + "  ]\n"
            + "}",
        ReplaceQueryPartsHint.of("from", "to").toComment());
    assertEquals(
        "{\n"
            + "  \"spanner_replacements\": [\n"
            + "    {\n"
            + "      \"regex\": \"from1\",\n"
            + "      \"replacement\": \"to1\",\n"
            + "      \"replace_mode\": \"ALL\"\n"
            + "    }\n"
            + "  ]\n"
            + "}",
        ReplaceQueryPartsHint.of("from1", "to1").toComment());
    assertEquals(
        "{\n"
            + "  \"spanner_replacements\": [\n"
            + "    {\n"
            + "      \"regex\": \"from1\",\n"
            + "      \"replacement\": \"to1\",\n"
            + "      \"replace_mode\": \"FIRST\"\n"
            + "    }\n"
            + "  ]\n"
            + "}",
        ReplaceQueryPartsHint.of("from1", "to1", ReplaceMode.FIRST).toComment());
  }

  @Test
  public void testFromComment() {
    assertEquals(
        ReplaceQueryPartsHint.of(" from singers ", " from singers@{force_index=foo} "),
        ReplaceQueryPartsHint.fromComment(
            "{spanner_replacements: ["
                + "{regex: \" from singers \", replacement: \" from singers@{force_index=foo} \"}"
                + "]}"));
    assertEquals(
        ReplaceQueryPartsHint.of(
            " from singers ", " from singers@{force_index=foo} ", ReplaceMode.ALL),
        ReplaceQueryPartsHint.fromComment(
            "{spanner_replacements: ["
                + "{regex: \" from singers \", replacement: \" from singers@{force_index=foo} \"}]}"));
    assertEquals(
        ReplaceQueryPartsHint.of(
            " from singers ", " from singers@{force_index=foo} ", ReplaceMode.FIRST),
        ReplaceQueryPartsHint.fromComment(
            "{spanner_replacements: [{"
                + "regex: \" from singers \", "
                + "replacement: \" from singers@{force_index=foo} \", "
                + "replace_mode: \"FIRST\"}]}"));
  }

  @Test
  public void testRoundTrip() {
    assertRoundTrip(ReplaceQueryPartsHint.of("from", "to"));
    assertRoundTrip(
        ReplaceQueryPartsHint.of(
            "select *\nfrom foo\nwhere true", "select *\nfrom bar\nwhere false"));
  }

  @Test
  public void testForceIndex() {
    assertEquals(
        "{\n"
            + "  \"spanner_replacements\": [\n"
            + "    {\n"
            + "      \"regex\": \" from singers \",\n"
            + "      \"replacement\": \" from singers @{FORCE_INDEX=idx_singers_active} \",\n"
            + "      \"replace_mode\": \"ALL\"\n"
            + "    }\n"
            + "  ]\n"
            + "}",
        Hints.forceIndexFrom("singers", "idx_singers_active", ReplaceMode.ALL).toComment());
    assertEquals(
        "{\n"
            + "  \"spanner_replacements\": [\n"
            + "    {\n"
            + "      \"regex\": \" join singers \",\n"
            + "      \"replacement\": \" join singers @{FORCE_INDEX=idx_singers_active} \",\n"
            + "      \"replace_mode\": \"ALL\"\n"
            + "    }\n"
            + "  ]\n"
            + "}",
        Hints.forceIndexJoin("singers", "idx_singers_active", ReplaceMode.ALL).toComment());
    assertEquals(
        "{\n"
            + "  \"spanner_replacements\": [\n"
            + "    {\n"
            + "      \"regex\": \" from singers \",\n"
            + "      \"replacement\": \" from singers @{FORCE_INDEX=idx_singers_active} \",\n"
            + "      \"replace_mode\": \"ALL\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"regex\": \" join singers \",\n"
            + "      \"replacement\": \" join singers @{FORCE_INDEX=idx_singers_active} \",\n"
            + "      \"replace_mode\": \"ALL\"\n"
            + "    }\n"
            + "  ]\n"
            + "}",
        Hints.forceIndexFrom("singers", "idx_singers_active", ReplaceMode.ALL)
            .combine(Hints.forceIndexJoin("singers", "idx_singers_active", ReplaceMode.ALL))
            .toComment());
  }

  @Test
  public void testReplace() {
    String inputSql =
        "select * "
            + "from singers s1 "
            + "inner join singers s2 on s1.id=s2.id "
            + "where s1.last_name like 'foo%' "
            + "and s1.id in (select s3.id from singers s3 where active=true)";
    assertEquals(
        "select * "
            + "from singers@{force_index=idx} s1 "
            + "inner join singers s2 on s1.id=s2.id "
            + "where s1.last_name like 'foo%' "
            + "and s1.id in (select s3.id from singers@{force_index=idx} s3 where active=true)",
        ReplaceQueryPartsHint.of(" from singers ", " from singers@{force_index=idx} ")
            .replace(inputSql));
    assertEquals(
        "select * "
            + "from singers@{force_index=idx} s1 "
            + "inner join singers s2 on s1.id=s2.id "
            + "where s1.last_name like 'foo%' "
            + "and s1.id in (select s3.id from singers s3 where active=true)",
        ReplaceQueryPartsHint.of(
                " from singers ", " from singers@{force_index=idx} ", ReplaceMode.FIRST)
            .replace(inputSql));
    assertEquals(
        "select * "
            + "from singers@{force_index=idx} s1 "
            + "inner join singers s2 on s1.id=s2.id "
            + "where s1.last_name like 'foo%'",
        ReplaceQueryPartsHint.of(
                " from singers ", " from singers@{force_index=idx} ", ReplaceMode.FIRST)
            .combine(
                ReplaceQueryPartsHint.of(
                    " and s1.id in \\(select s3.id from singers s3 where active=true\\)",
                    "",
                    ReplaceMode.FIRST))
            .replace(inputSql));
    assertEquals(
        "select * "
            + "from singers s1 "
            + "inner join singers s2 on s1.id=s2.id "
            + "where s1.last_name like 'foo%'",
        ReplaceQueryPartsHint.of(
                " and s1.id in \\(select s3.id from singers s3 where active=true\\)",
                "",
                ReplaceMode.FIRST)
            .replace(inputSql));
  }

  private void assertRoundTrip(ReplaceQueryPartsHint hint) {
    assertEquals(hint, ReplaceQueryPartsHint.fromComment(hint.toComment()));
  }
}
