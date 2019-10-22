package com.google.cloud.spanner.hibernate;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates and correctly formats Spanner query hints provided through Hibernate.
 */
final class QueryHintProcessor {

  /**
   * Set of all supported Spanner Join hints.
   */
  private static final Set<String> JOIN_HINT_KEYS = CollectionUtils.setOf(
      "FORCE_JOIN_ORDER",
      "JOIN_METHOD"
  );

  /**
   * The pattern that each hint should be written in; i.e. "HINT_KEY=HINT_VALUE".
   */
  private static final Pattern HINT_PATTERN = Pattern.compile("([a-zA-Z_]+)=([a-zA-Z_]+)");

  static String formatQueryHints(List<String> queryHints) {
    if (queryHints.isEmpty()) {
      return "";
    }

    for (String hint : queryHints) {
      if (!validateQueryHint(hint)) {
        throw new IllegalArgumentException(hint + " is not a valid/support Spanner query hint. "
            + "Supported Spanner hints include: " + JOIN_HINT_KEYS);
      }
    }

    String aggregatedHints = String.join(", ", queryHints);
    return String.format("@{%s}", aggregatedHints);
  }

  static boolean validateQueryHint(String hint) {
    Matcher patternMatcher = HINT_PATTERN.matcher(hint);

    if (patternMatcher.matches() && patternMatcher.groupCount() == 2) {
      String hintKey = patternMatcher.group(1);
      String hintValue = patternMatcher.group(2);

      if (isValidHintKey(hintKey) && !hintValue.isEmpty()) {
        return true;
      }
    }

    return false;
  }

  private static boolean isValidHintKey(String hintKey) {
    return JOIN_HINT_KEYS.stream().anyMatch(key -> hintKey.equalsIgnoreCase(key));
  }
}
