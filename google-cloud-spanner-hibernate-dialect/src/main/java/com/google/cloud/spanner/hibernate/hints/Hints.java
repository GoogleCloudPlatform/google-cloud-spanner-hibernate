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

import com.google.cloud.spanner.hibernate.hints.ReplaceQueryPartsHint.ReplaceMode;
import javax.annotation.Nullable;

/**
 * Utility class for building various query hints for Cloud Spanner.
 *
 * <p>All hints in this class can also be created manually using the
 * {@link ReplaceQueryPartsHint} class.
 *
 * <p>Usage with {@link org.hibernate.query.Query}:
 *
 * <pre>{@code
 * CriteriaBuilder cb = session.getCriteriaBuilder();
 * CriteriaQuery<Singer> cr = cb.createQuery(Singer.class);
 * Root<Singer> root = cr.from(Singer.class);
 * root.join("albums", JoinType.LEFT);
 * cr.select(root);
 * Query<Singer> query = session.createQuery(cr)
 *     .addQueryHint(
 *         Hints.forceIndexFrom("singer", "idx_singer_active", ReplaceMode.ALL).toQueryHint())
 *     .addQueryHint(
 *         Hints.forceIndexJoin("album", "idx_album_title", ReplaceMode.ALL).toQueryHint());
 * }</pre>
 *
 * <p>Usage with {@link jakarta.persistence.TypedQuery}:
 *
 * <pre>{@code
 * CriteriaBuilder cb = entityManager.getCriteriaBuilder();
 * CriteriaQuery<Singer> cr = cb.createQuery(Singer.class);
 * Root<Singer> root = cr.from(Singer.class);
 * root.join("albums", JoinType.LEFT);
 * cr.select(root);
 * TypedQuery<Singer> query = entityManager.createQuery(cr)
 *     .setHint(AvailableHints.HINT_COMMENT,
 *         Hints.forceIndexFrom("singer", "idx_singer_active", ReplaceMode.ALl)
 *             .combine(Hints.forceIndexJoin("album", "idx_album_title", ReplaceMode.ALL))
 *             .toComment());
 * }</pre>
 *
 * <p>Usage with the {@link jakarta.persistence.QueryHint} annotation:
 *
 * <pre>{@code
 * // The hint value used here is generated by calling the method:
 * // Hints.forceIndexFrom("singer", "idx_singer_active").toComment()
 * @QueryHints({@QueryHint(name = AvailableHints.HINT_COMMENT, value = "{\n"
 *       + "  \"spanner_replacements\": [\n"
 *       + "    {\n"
 *       + "      \"regex\": \" from singer \",\n"
 *       + "      \"replacement\": \" from singer @{FORCE_INDEX=idx_singer_active} \",\n"
 *       + "      \"replace_mode\": \"ALL\"\n"
 *       + "    }\n"
 *       + "  ]\n"
 *       + "}")})
 * List<Singer> findByActive(boolean active);
 * }</pre>
 */
public class Hints {
  /** Possible values for the LOCK_SCANNED_RANGES hint. */
  public enum LockScannedRanges {
    EXCLUSIVE,
    SHARED,
  }

  /** Possible values for the SCAN_METHOD table hint. */
  public enum ScanMethod {
    AUTO,
    BATCH,
    ROW,
  }

  /** Possible values for the INDEX_STRATEGY table hint. */
  public enum IndexStrategy {
    FORCE_INDEX_UNION,
  }

  /** Possible values for the JOIN_METHOD hint. */
  public enum JoinMethod {
    HASH_JOIN,
    APPLY_JOIN,
    MERGE_JOIN,
    PUSH_BROADCAST_HASH_JOIN,
  }

  /** Possible values for the HASH_JOIN_BUILD_SIDE hint. */
  public enum HashJoinBuildSide {
    BUILD_LEFT,
    BUILD_RIGHT,
  }

  /** Possible values for the HASH_JOIN_EXECUTION hint. */
  public enum HashJoinExecution {
    MULTI_PASS,
    ONE_PASS,
  }

  private static final String SELECT_OR_DML = "(?i)(?:^|\\s)(select|insert|update|delete)";

  /**
   * Creates a hint that will prepend '@{hint=value}' to the first occurrence of regex.
   */
  private static ReplaceQueryPartsHint statementHint(String hint, Object value) {
    return ReplaceQueryPartsHint.of(
        SELECT_OR_DML, String.format("@{%s=%s}$1", hint, value), ReplaceMode.FIRST);
  }

  /**
   * Creates a hint that adds @{USE_ADDITIONAL_PARALLELISM=value} to the statement.
   *
   * @param value the hint value
   * @return a hint that can be added as a comment or query hint to a Hibernate statement
   */
  public static ReplaceQueryPartsHint useAdditionalParallelism(boolean value) {
    return statementHint("USE_ADDITIONAL_PARALLELISM", value);
  }

  /**
   * Creates a hint that adds @{OPTIMIZER_VERSION=version} to the statement.
   *
   * @param version the optimizer version to use
   * @return a hint that can be added as a comment or query hint to a Hibernate statement
   */
  public static ReplaceQueryPartsHint optimizerVersion(String version) {
    return statementHint("OPTIMIZER_VERSION", version);
  }

  /**
   * Creates a hint that adds @{OPTIMIZER_STATISTICS_PACKAGE=value} to the statement.
   *
   * @param value the optimizer package to use
   * @return a hint that can be added as a comment or query hint to a Hibernate statement
   */
  public static ReplaceQueryPartsHint optimizerStatisticsPackage(String value) {
    return statementHint("OPTIMIZER_STATISTICS_PACKAGE", value);
  }

  /**
   * Creates a hint that adds @{ALLOW_DISTRIBUTED_MERGE=value} to the statement.
   *
   * @param value the hint value
   * @return a hint that can be added as a comment or query hint to a Hibernate statement
   */
  public static ReplaceQueryPartsHint allowDistributedMerge(boolean value) {
    return statementHint("ALLOW_DISTRIBUTED_MERGE", value);
  }

  /**
   * Creates a hint that adds @{LOCK_SCANNED_RANGES=value} to the statement.
   *
   * @param value the hint value
   * @return a hint that can be added as a comment or query hint to a Hibernate statement
   */
  public static ReplaceQueryPartsHint lockScannedRanges(LockScannedRanges value) {
    return statementHint("LOCK_SCANNED_RANGES", value);
  }

  /**
   * Creates a hint that adds @{SCAN_METHOD=value} to the statement.
   *
   * @param value the hint value
   * @return a hint that can be added as a comment or query hint to a Hibernate statement
   */
  public static ReplaceQueryPartsHint scanMethod(ScanMethod value) {
    return statementHint("SCAN_METHOD", value);
  }

  /**
   * Creates a hint that adds @{FORCE_STREAMABLE=value} to the first SELECT clause.
   *
   * @param value the hint value
   * @return a hint that can be added as a comment or query hint to a Hibernate statement
   */
  public static ReplaceQueryPartsHint forceStreamable(boolean value) {
    return ReplaceQueryPartsHint.of(
        "(?i)(^|\\s)(select)",
        "$1$2 @{FORCE_STREAMABLE=" + value + "}",
        ReplaceMode.FIRST);
  }

  /**
   * Creates a hint that adds @{PREFER_STREAMABLE=value} to the first SELECT clause.
   *
   * @param value the hint value
   * @return a hint that can be added as a comment or query hint to a Hibernate statement
   */
  public static ReplaceQueryPartsHint preferStreamable(boolean value) {
    return ReplaceQueryPartsHint.of(
        "(?i)(^|\\s)(select)",
        "$1$2@{PREFER_STREAMABLE=" + value + "}",
        ReplaceMode.FIRST);
  }

  /**
   * Creates a hint that replaces occurrences of <code>from table</code> with <code>from
   * table@{FORCE_INDEX=index}</code>.
   *
   * <p>Combine this hint with a {@link #forceIndexJoin(String, String, ReplaceMode)} hint if
   * you want to add a FORCE_INDEX hint to both <code>from table</code> and <code>join table</code>
   * occurrences in the query.
   *
   * @param table the table name to annotate with the FORCE_INDEX hint.
   * @param index the index name to add to the FORCE_INDEX hint.
   * @param replaceMode whether to replace all or only the first occurrence in the query.
   * @return a hint that can be added as a comment or query hint to a Hibernate query
   */
  public static ReplaceQueryPartsHint forceIndexFrom(
      String table, String index, ReplaceMode replaceMode) {
    return ForceIndexHint.from(table, index, replaceMode);
  }

  /**
   * Creates a hint that replaces occurrences of <code>join table</code> with <code>join
   * table@{FORCE_INDEX=index}</code>.
   *
   * <p>Combine this hint with a {@link #forceIndexFrom(String, String, ReplaceMode)} hint if
   * you want to add a FORCE_INDEX hint to both <code>from table</code> and <code>join table</code>
   * occurrences in the query.
   *
   * @param table the table name to annotate with the FORCE_INDEX hint.
   * @param index the index name to add to the FORCE_INDEX hint.
   * @param replaceMode whether to replace all or only the first occurrence in the query.
   * @return a hint that can be added as a comment or query hint to a Hibernate query
   */
  public static ReplaceQueryPartsHint forceIndexJoin(
      String table, String index, ReplaceMode replaceMode) {
    return ForceIndexHint.join(table, index, replaceMode);
  }

  /**
   * Creates a hint that replaces occurrences of <code>from table</code> with <code>from
   * table@{GROUPBY_SCAN_OPTIMIZATION=value}</code>.
   *
   * <p>Combine this hint with a
   * {@link #groupByScanOptimizationJoin(String, boolean, ReplaceMode)} hint if you want to add
   * a GROUPBY_SCAN_OPTIMIZATION hint to both <code>from table</code> and <code>join table</code>
   * occurrences in the query.
   *
   * @param table the table name to annotate with the GROUPBY_SCAN_OPTIMIZATION hint.
   * @param value the hint value.
   * @param replaceMode whether to replace all or only the first occurrence in the query.
   * @return a hint that can be added as a comment or query hint to a Hibernate query
   */
  public static ReplaceQueryPartsHint groupByScanOptimizationFrom(
      String table, boolean value, ReplaceMode replaceMode) {
    return GroupByScanOptimizationHint.from(table, value, replaceMode);
  }

  /**
   * Creates a hint that replaces occurrences of <code>join table</code> with <code>join
   * table@{GROUPBY_SCAN_OPTIMIZATION=value}</code>.
   *
   * <p>Combine this hint with a
   * {@link #groupByScanOptimizationFrom(String, boolean, ReplaceMode)} hint if you want to add
   * a GROUPBY_SCAN_OPTIMIZATION hint to both <code>from table</code> and <code>join table</code>
   * occurrences in the query.
   *
   * @param table the table name to annotate with the GROUPBY_SCAN_OPTIMIZATION hint.
   * @param value the hint value.
   * @param replaceMode whether to replace all or only the first occurrence in the query.
   * @return a hint that can be added as a comment or query hint to a Hibernate query
   */
  public static ReplaceQueryPartsHint groupByScanOptimizationJoin(
      String table, boolean value, ReplaceMode replaceMode) {
    return GroupByScanOptimizationHint.join(table, value, replaceMode);
  }

  /**
   * Creates a hint that replaces occurrences of <code>from table</code> with <code>from
   * table@{SCAN_METHOD=value}</code>.
   *
   * <p>Combine this hint with a
   * {@link #scanMethodJoin(String, ScanMethod, ReplaceMode)} hint if you want to add
   * a SCAN_METHOD hint to both <code>from table</code> and <code>join table</code>
   * occurrences in the query.
   *
   * @param table the table name to annotate with the SCAN_METHOD hint.
   * @param value the hint value.
   * @param replaceMode whether to replace all or only the first occurrence in the query.
   * @return a hint that can be added as a comment or query hint to a Hibernate query
   */
  public static ReplaceQueryPartsHint scanMethodFrom(
      String table, ScanMethod value, ReplaceMode replaceMode) {
    return ScanMethodHint.from(table, value, replaceMode);
  }

  /**
   * Creates a hint that replaces occurrences of <code>join table</code> with <code>join
   * table@{SCAN_METHOD=value}</code>.
   *
   * <p>Combine this hint with a
   * {@link #scanMethodFrom(String, ScanMethod, ReplaceMode)} hint if you want to add
   * a SCAN_METHOD hint to both <code>from table</code> and <code>join table</code>
   * occurrences in the query.
   *
   * @param table the table name to annotate with the SCAN_METHOD hint.
   * @param value the hint value.
   * @param replaceMode whether to replace all or only the first occurrence in the query.
   * @return a hint that can be added as a comment or query hint to a Hibernate query
   */
  public static ReplaceQueryPartsHint scanMethodJoin(
      String table, ScanMethod value, ReplaceMode replaceMode) {
    return ScanMethodHint.join(table, value, replaceMode);
  }

  /**
   * Creates a hint that replaces occurrences of <code>from table</code> with <code>from
   * table@{INDEX_STRATEGY=value}</code>.
   *
   * <p>Combine this hint with a
   * {@link #indexStrategyJoin(String, IndexStrategy, ReplaceMode)} hint if you want to add
   * a INDEX_STRATEGY hint to both <code>from table</code> and <code>join table</code>
   * occurrences in the query.
   *
   * @param table the table name to annotate with the INDEX_STRATEGY hint.
   * @param value the hint value.
   * @param replaceMode whether to replace all or only the first occurrence in the query.
   * @return a hint that can be added as a comment or query hint to a Hibernate query
   */
  public static ReplaceQueryPartsHint indexStrategyFrom(
      String table, IndexStrategy value, ReplaceMode replaceMode) {
    return IndexStrategyHint.from(table, value, replaceMode);
  }

  /**
   * Creates a hint that replaces occurrences of <code>join table</code> with <code>join
   * table@{INDEX_STRATEGY=value}</code>.
   *
   * <p>Combine this hint with a
   * {@link #indexStrategyFrom(String, IndexStrategy, ReplaceMode)} hint if you want to add
   * a INDEX_STRATEGY hint to both <code>from table</code> and <code>join table</code>
   * occurrences in the query.
   *
   * @param table the table name to annotate with the INDEX_STRATEGY hint.
   * @param value the hint value.
   * @param replaceMode whether to replace all or only the first occurrence in the query.
   * @return a hint that can be added as a comment or query hint to a Hibernate query
   */
  public static ReplaceQueryPartsHint indexStrategyJoin(
      String table, IndexStrategy value, ReplaceMode replaceMode) {
    return IndexStrategyHint.join(table, value, replaceMode);
  }

  /**
   * Creates a hint that replaces occurrences of <code>join table</code> with
   * <code>join@{FORCE_JOIN_ORDER=value} table</code>.
   *
   * @param table the table name to annotate with the FORCE_JOIN_ORDER hint.
   * @param value the hint value.
   * @param replaceMode whether to replace all or only the first occurrence in the query.
   * @return a hint that can be added as a comment or query hint to a Hibernate query
   */
  public static ReplaceQueryPartsHint forceJoinOrder(
      String table, boolean value, ReplaceMode replaceMode) {
    return ReplaceQueryPartsHint.of(
        join(table),
        " join@{FORCE_JOIN_ORDER=" + value + "} " + table + " ",
        replaceMode);
  }

  /**
   * Creates a hint that replaces occurrences of <code>join table</code> with
   * <code>join@{JOIN_METHOD=value} table</code>.
   *
   * @param table the table name to annotate with the JOIN_METHOD hint.
   * @param value the hint value.
   * @param replaceMode whether to replace all or only the first occurrence in the query.
   * @return a hint that can be added as a comment or query hint to a Hibernate query
   */
  public static ReplaceQueryPartsHint joinMethod(
      String table, JoinMethod value, ReplaceMode replaceMode) {
    return ReplaceQueryPartsHint.of(
        join(table),
        " join@{JOIN_METHOD=" + value.name() + "} " + table + " ",
        replaceMode);
  }

  /**
   * Creates a hint that replaces occurrences of <code>join table</code> with
   * <code>
   * join@{JOIN_METHOD=HASH_JOIN
   *     [, HASH_JOIN_BUILD_SIDE=buildSide]
   *     [, HASH_JOIN_EXECUTION=execution]}
   * table
   * </code>.
   *
   * @param table the table name to annotate with the HASH_JOIN_BUILD_SIDE hint.
   * @param hashJoinBuildSide the build side hint value.
   * @param hashJoinExecution the execution hint value.
   * @param replaceMode whether to replace all or only the first occurrence in the query.
   * @return a hint that can be added as a comment or query hint to a Hibernate query
   */
  public static ReplaceQueryPartsHint hashJoin(
      String table,
      @Nullable HashJoinBuildSide hashJoinBuildSide,
      @Nullable HashJoinExecution hashJoinExecution,
      ReplaceMode replaceMode) {
    String replacement = " join@{JOIN_METHOD=HASH_JOIN";
    if (hashJoinBuildSide != null) {
      replacement += ", HASH_JOIN_BUILD_SIDE=" + hashJoinBuildSide.name();
    }
    if (hashJoinExecution != null) {
      replacement += ", HASH_JOIN_EXECUTION=" + hashJoinExecution.name();
    }
    replacement += "} " + table + " ";
    return ReplaceQueryPartsHint.of(join(table), replacement, replaceMode);
  }

  /**
   * Creates a hint that replaces occurrences of <code>join table</code> with
   * <code>join@{JOIN_METHOD=HASH_JOIN, HASH_JOIN_BUILD_SIDE=value} table</code>.
   *
   * <p><strong>This hint also automatically adds JOIN_METHOD=HASH_JOIN.</strong>
   *
   * @param table the table name to annotate with the HASH_JOIN_BUILD_SIDE hint.
   * @param value the hint value.
   * @param replaceMode whether to replace all or only the first occurrence in the query.
   * @return a hint that can be added as a comment or query hint to a Hibernate query
   */
  public static ReplaceQueryPartsHint hashJoinBuildSide(
      String table, HashJoinBuildSide value, ReplaceMode replaceMode) {
    return ReplaceQueryPartsHint.of(
        join(table),
        " join@{JOIN_METHOD=HASH_JOIN, HASH_JOIN_BUILD_SIDE="
            + value.name() + "} " + table + " ",
        replaceMode);
  }

  /**
   * Creates a hint that replaces occurrences of <code>join table</code> with
   * <code>join@{JOIN_METHOD=HASH_JOIN, HASH_JOIN_EXECUTION=value} table</code>.
   *
   * <p><strong>This hint also automatically adds JOIN_METHOD=HASH_JOIN.</strong>
   *
   * @param table the table name to annotate with the HASH_JOIN_EXECUTION hint.
   * @param value the hint value.
   * @param replaceMode whether to replace all or only the first occurrence in the query.
   * @return a hint that can be added as a comment or query hint to a Hibernate query
   */
  public static ReplaceQueryPartsHint hashJoinExecution(
      String table, HashJoinExecution value, ReplaceMode replaceMode) {
    return ReplaceQueryPartsHint.of(
        join(table),
        " join@{JOIN_METHOD=HASH_JOIN, HASH_JOIN_EXECUTION="
            + value.name()
            + "} " + table + " ",
        replaceMode);
  }

  /**
   * Creates a hint that replaces occurrences of <code>join table</code> with
   * <code>join@{JOIN_METHOD=APPLY_JOIN, BATCH_MODE=value} table</code>.
   *
   * <p><strong>This hint also automatically adds JOIN_METHOD=APPLY_JOIN.</strong>
   *
   * @param table the table name to annotate with the BATCH_MODE hint.
   * @param value the hint value.
   * @param replaceMode whether to replace all or only the first occurrence in the query.
   * @return a hint that can be added as a comment or query hint to a Hibernate query
   */
  public static ReplaceQueryPartsHint batchMode(
      String table, boolean value, ReplaceMode replaceMode) {
    return ReplaceQueryPartsHint.of(
        join(table),
        " join@{JOIN_METHOD=APPLY_JOIN, BATCH_MODE=" + value + "} " + table + " ",
        replaceMode);
  }

  /** Returns a ' from table ' string. */
  static String from(String table) {
    return " from " + table + " ";
  }

  /** Returns a ' join table ' string. */
  static String join(String table) {
    return " join " + table + " ";
  }

  /** Returns a ' from table @{hint=value} ' string. */
  static String fromTableHint(String table, String hint, String value) {
    return from(table) + "@{" + hint + '=' + value + "} ";
  }

  /** Returns a ' join table @{hint=value} ' string. */
  static String joinTableHint(String table, String hint, String value) {
    return join(table) + "@{" + hint + '=' + value + "} ";
  }

}
