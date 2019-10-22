package com.google.cloud.spanner.hibernate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class to more easily build collections inline code.
 */
final class CollectionUtils {

  /**
   * Returns a set of the provided {@code elements}.
   */
  static <T> List<T> listOf(T... elements) {
    ArrayList<T> result = new ArrayList<>();
    for (T element : elements) {
      result.add(element);
    }
    return result;
  }

  /**
   * Returns a set of the provided {@code elements}.
   */
  static <T> Set<T> setOf(T... elements) {
    HashSet<T> result = new HashSet<>();
    for (T element : elements) {
      result.add(element);
    }
    return result;
  }
}
