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

import com.google.cloud.spanner.hibernate.hints.Hints.HashJoinBuildSide;
import com.google.cloud.spanner.hibernate.hints.Hints.HashJoinExecution;
import com.google.cloud.spanner.hibernate.hints.Hints.IndexStrategy;
import com.google.cloud.spanner.hibernate.hints.Hints.LockScannedRanges;
import com.google.cloud.spanner.hibernate.hints.Hints.ScanMethod;
import com.google.cloud.spanner.hibernate.hints.ReplaceQueryPartsHint.ReplaceMode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for the {@link Hints} util class. */
@RunWith(JUnit4.class)
public class HintsTest {

  @Test
  public void testForceIndexFrom() {
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
  }

  @Test
  public void testForceIndexJoin() {
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
  }

  @Test
  public void testGroupByScanOptimizationFrom() {
    assertEquals(
        "{\n"
            + "  \"spanner_replacements\": [\n"
            + "    {\n"
            + "      \"regex\": \" from singers \",\n"
            + "      \"replacement\": \" from singers @{GROUPBY_SCAN_OPTIMIZATION=true} \",\n"
            + "      \"replace_mode\": \"ALL\"\n"
            + "    }\n"
            + "  ]\n"
            + "}",
        Hints.groupByScanOptimizationFrom("singers", true, ReplaceMode.ALL).toComment());
  }

  @Test
  public void testGroupByScanOptimizationJoin() {
    assertEquals(
        "{\n"
            + "  \"spanner_replacements\": [\n"
            + "    {\n"
            + "      \"regex\": \" join singers \",\n"
            + "      \"replacement\": \" join singers @{GROUPBY_SCAN_OPTIMIZATION=false} \",\n"
            + "      \"replace_mode\": \"ALL\"\n"
            + "    }\n"
            + "  ]\n"
            + "}",
        Hints.groupByScanOptimizationJoin("singers", false, ReplaceMode.ALL).toComment());
  }

  @Test
  public void testScanMethodFrom() {
    assertEquals(
        "{\n"
            + "  \"spanner_replacements\": [\n"
            + "    {\n"
            + "      \"regex\": \" from singers \",\n"
            + "      \"replacement\": \" from singers @{SCAN_METHOD=BATCH} \",\n"
            + "      \"replace_mode\": \"ALL\"\n"
            + "    }\n"
            + "  ]\n"
            + "}",
        Hints.scanMethodFrom("singers", ScanMethod.BATCH, ReplaceMode.ALL).toComment());
  }

  @Test
  public void testScanMethodJoin() {
    assertEquals(
        "{\n"
            + "  \"spanner_replacements\": [\n"
            + "    {\n"
            + "      \"regex\": \" join singers \",\n"
            + "      \"replacement\": \" join singers @{SCAN_METHOD=ROW} \",\n"
            + "      \"replace_mode\": \"ALL\"\n"
            + "    }\n"
            + "  ]\n"
            + "}",
        Hints.scanMethodJoin("singers", ScanMethod.ROW, ReplaceMode.ALL).toComment());
  }

  @Test
  public void testIndexStrategyFrom() {
    assertEquals(
        "{\n"
            + "  \"spanner_replacements\": [\n"
            + "    {\n"
            + "      \"regex\": \" from singers \",\n"
            + "      \"replacement\": \" from singers @{INDEX_STRATEGY=FORCE_INDEX_UNION} \",\n"
            + "      \"replace_mode\": \"ALL\"\n"
            + "    }\n"
            + "  ]\n"
            + "}",
        Hints.indexStrategyFrom("singers", IndexStrategy.FORCE_INDEX_UNION, ReplaceMode.ALL)
            .toComment());
  }

  @Test
  public void testIndexStrategyJoin() {
    assertEquals(
        "{\n"
            + "  \"spanner_replacements\": [\n"
            + "    {\n"
            + "      \"regex\": \" join singers \",\n"
            + "      \"replacement\": \" join singers @{INDEX_STRATEGY=FORCE_INDEX_UNION} \",\n"
            + "      \"replace_mode\": \"ALL\"\n"
            + "    }\n"
            + "  ]\n"
            + "}",
        Hints.indexStrategyJoin("singers", IndexStrategy.FORCE_INDEX_UNION, ReplaceMode.ALL)
            .toComment());
  }

  @Test
  public void testForceJoinOrder() {
    assertEquals(
        "{\n"
            + "  \"spanner_replacements\": [\n"
            + "    {\n"
            + "      \"regex\": \" join singers \",\n"
            + "      \"replacement\": \" join@{FORCE_JOIN_ORDER=true} singers \",\n"
            + "      \"replace_mode\": \"ALL\"\n"
            + "    }\n"
            + "  ]\n"
            + "}",
        Hints.forceJoinOrder("singers", true, ReplaceMode.ALL).toComment());
  }

  @Test
  public void testStatementTag() {
    assertEquals(
        "@{STATEMENT_TAG=my_tag}select * from singers",
        Hints.statementTag("my_tag").replace("select * from singers"));
    assertEquals(
        "@{STATEMENT_TAG=other_tag}SELECT * from singers",
        Hints.statementTag("other_tag").replace("SELECT * from singers"));
    assertEquals(
        "@{STATEMENT_TAG=insert_singer}" + "insert into singers (id, value) SELECT * from singers",
        Hints.statementTag("insert_singer")
            .replace("insert into singers (id, value) SELECT * from singers"));

    System.out.println(Hints.statementTag("get_albums_by_title").toComment());
  }

  @Test
  public void testUseAdditionalParallelism() {
    assertEquals(
        "@{USE_ADDITIONAL_PARALLELISM=true}select * from singers",
        Hints.useAdditionalParallelism(true).replace("select * from singers"));
    assertEquals(
        "@{USE_ADDITIONAL_PARALLELISM=true}SELECT * from singers",
        Hints.useAdditionalParallelism(true).replace("SELECT * from singers"));
    assertEquals(
        "@{USE_ADDITIONAL_PARALLELISM=false}"
            + "insert into singers (id, value) SELECT * from singers",
        Hints.useAdditionalParallelism(false)
            .replace("insert into singers (id, value) SELECT * from singers"));
  }

  @Test
  public void testOptimizerVersion() {
    assertEquals(
        "@{OPTIMIZER_VERSION=1}select * from singers",
        Hints.optimizerVersion("1").replace("select * from singers"));
    assertEquals(
        "@{OPTIMIZER_VERSION=latest_version}SELECT * from singers",
        Hints.optimizerVersion("latest_version").replace("SELECT * from singers"));
    assertEquals(
        "@{OPTIMIZER_VERSION=default_version}"
            + "insert into singers (id, value) SELECT * from singers",
        Hints.optimizerVersion("default_version")
            .replace("insert into singers (id, value) SELECT * from singers"));
  }

  @Test
  public void testOptimizerStatisticsPackage() {
    assertEquals(
        "@{OPTIMIZER_STATISTICS_PACKAGE=2024_02_27_10_00_00}select * from singers",
        Hints.optimizerStatisticsPackage("2024_02_27_10_00_00").replace("select * from singers"));
    assertEquals(
        "@{OPTIMIZER_STATISTICS_PACKAGE=2024_02_27_10_00_00}SELECT * from singers",
        Hints.optimizerStatisticsPackage("2024_02_27_10_00_00").replace("SELECT * from singers"));
    assertEquals(
        "@{OPTIMIZER_STATISTICS_PACKAGE=latest}"
            + "insert into singers (id, value) SELECT * from singers",
        Hints.optimizerStatisticsPackage("latest")
            .replace("insert into singers (id, value) SELECT * from singers"));
  }

  @Test
  public void testAllowDistributedMerge() {
    assertEquals(
        "@{ALLOW_DISTRIBUTED_MERGE=true}select * from singers",
        Hints.allowDistributedMerge(true).replace("select * from singers"));
    assertEquals(
        "@{ALLOW_DISTRIBUTED_MERGE=true}SELECT * from singers",
        Hints.allowDistributedMerge(true).replace("SELECT * from singers"));
    assertEquals(
        "@{ALLOW_DISTRIBUTED_MERGE=false}"
            + "insert into singers (id, value) SELECT * from singers",
        Hints.allowDistributedMerge(false)
            .replace("insert into singers (id, value) SELECT * from singers"));
  }

  @Test
  public void testLockScannedRanges() {
    assertEquals(
        "@{LOCK_SCANNED_RANGES=EXCLUSIVE}select * from singers",
        Hints.lockScannedRanges(LockScannedRanges.EXCLUSIVE).replace("select * from singers"));
    assertEquals(
        "@{LOCK_SCANNED_RANGES=EXCLUSIVE}SELECT * from singers",
        Hints.lockScannedRanges(LockScannedRanges.EXCLUSIVE).replace("SELECT * from singers"));
    assertEquals(
        "@{LOCK_SCANNED_RANGES=SHARED}" + "insert into singers (id, value) SELECT * from singers",
        Hints.lockScannedRanges(LockScannedRanges.SHARED)
            .replace("insert into singers (id, value) SELECT * from singers"));
  }

  @Test
  public void testScanMethod() {
    assertEquals(
        "@{SCAN_METHOD=ROW}select * from singers",
        Hints.scanMethod(ScanMethod.ROW).replace("select * from singers"));
    assertEquals(
        "@{SCAN_METHOD=BATCH}SELECT * from singers",
        Hints.scanMethod(ScanMethod.BATCH).replace("SELECT * from singers"));
    assertEquals(
        "@{SCAN_METHOD=AUTO}" + "insert into singers (id, value) SELECT * from singers",
        Hints.scanMethod(ScanMethod.AUTO)
            .replace("insert into singers (id, value) SELECT * from singers"));
  }

  @Test
  public void testForceStreamable() {
    assertEquals(
        "select @{FORCE_STREAMABLE=true} * from singers",
        Hints.forceStreamable(true).replace("select * from singers"));
    assertEquals(
        "SELECT @{FORCE_STREAMABLE=false} * from singers",
        Hints.forceStreamable(false).replace("SELECT * from singers"));
    assertEquals(
        "insert into singers (id, value) " + "SELECT @{FORCE_STREAMABLE=true} * from singers",
        Hints.forceStreamable(true)
            .replace("insert into singers (id, value) SELECT * from singers"));
  }

  @Test
  public void testPreferStreamable() {
    assertEquals(
        "select@{PREFER_STREAMABLE=true} * from singers",
        Hints.preferStreamable(true).replace("select * from singers"));
    assertEquals(
        "SELECT@{PREFER_STREAMABLE=false} * from singers",
        Hints.preferStreamable(false).replace("SELECT * from singers"));
    assertEquals(
        "insert into singers (id, value) " + "SELECT@{PREFER_STREAMABLE=true} * from singers",
        Hints.preferStreamable(true)
            .replace("insert into singers (id, value) SELECT * from singers"));
  }

  @Test
  public void testHashJoinBuildSide() {
    assertEquals(
        "select * from singers "
            + "inner join@{JOIN_METHOD=HASH_JOIN, HASH_JOIN_BUILD_SIDE=BUILD_RIGHT} albums "
            + "on albums.singer=singers.id",
        Hints.hashJoinBuildSide("albums", HashJoinBuildSide.BUILD_RIGHT, ReplaceMode.FIRST)
            .replace("select * from singers inner join albums on albums.singer=singers.id"));
  }

  @Test
  public void testHashJoinExecution() {
    assertEquals(
        "select * from singers "
            + "inner join@{JOIN_METHOD=HASH_JOIN, HASH_JOIN_EXECUTION=ONE_PASS} albums "
            + "on albums.singer=singers.id",
        Hints.hashJoinExecution("albums", HashJoinExecution.ONE_PASS, ReplaceMode.FIRST)
            .replace("select * from singers inner join albums on albums.singer=singers.id"));
    assertEquals(
        "select * from singers "
            + "inner join@{"
            + "JOIN_METHOD=HASH_JOIN, "
            + "HASH_JOIN_BUILD_SIDE=BUILD_LEFT, "
            + "HASH_JOIN_EXECUTION=ONE_PASS} "
            + "albums on albums.singer=singers.id",
        Hints.hashJoin(
                "albums",
                HashJoinBuildSide.BUILD_LEFT,
                HashJoinExecution.ONE_PASS,
                ReplaceMode.FIRST)
            .replace("select * from singers inner join albums on albums.singer=singers.id"));
  }

  @Test
  public void testBatchMode() {
    assertEquals(
        "select * from singers "
            + "inner join@{JOIN_METHOD=APPLY_JOIN, BATCH_MODE=true} albums "
            + "on albums.singer=singers.id",
        Hints.batchMode("albums", true, ReplaceMode.FIRST)
            .replace("select * from singers inner join albums on albums.singer=singers.id"));
  }
}
