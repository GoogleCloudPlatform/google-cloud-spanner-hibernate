package com.google.cloud.spanner.hibernate;

public class Hints {

  public static SpannerReplaceQueryPartsHint forceIndex(String table, String index) {
    return SpannerReplaceQueryPartsHint.of(
        from(table), fromForceIndex(table, index),
        join(table), joinForceIndex(table, index));
  }

  private static String from(String table) {
    return " from " + table + " ";
  }

  private static String fromForceIndex(String table, String index) {
    return " from " + table + "@{FORCE_INDEX=" + index + "} ";
  }

  private static String join(String table) {
    return " join " + table + " ";
  }

  private static String joinForceIndex(String table, String index) {
    return " join " + table + "@{FORCE_INDEX=" + index + "} ";
  }

}
