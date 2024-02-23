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

import com.google.api.client.json.JsonGenerator;
import com.google.api.client.json.gson.GsonFactory;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hibernate.HibernateException;

/**
 * Hint that can be used in combination with the {@link SpannerDialect} to replace specific parts of
 * a generated query. This can for example be used to replace a `FROM my_table` clause with one that
 * uses an index hint: `FROM my_table@{FORCE_INDEX=my_index}`.
 */
public class SpannerReplaceQueryPartsHint {
  static final String PREFIX = "spanner_replacements";

  private final Map<String, String> replacements;

  /** Re-creates a hint from a JSON-formatted comment. */
  static SpannerReplaceQueryPartsHint fromComment(String comment) {
    return fromJson(comment);
  }

  /**
   * Creates a hint that will replace all occurrences of {@code toBeReplaced} with
   * {@code replacement}.
   */
  public static SpannerReplaceQueryPartsHint of(String toBeReplaced, String replacement) {
    return new SpannerReplaceQueryPartsHint(ImmutableMap.of(toBeReplaced, replacement));
  }

  /**
   * Creates a hint that will replace all occurrences of {@code toBeReplaced1} with
   * {@code replacement1} and so on.
   */
  public static SpannerReplaceQueryPartsHint of(
      String toBeReplaced1, String replacement1,
      String toBeReplaced2, String replacement2) {
    return new SpannerReplaceQueryPartsHint(
        ImmutableMap.of(toBeReplaced1, replacement1, toBeReplaced2, replacement2));
  }

  /**
   * Creates a hint that will replace all occurrences of {@code toBeReplaced1} with
   * {@code replacement1} and so on.
   */
  public static SpannerReplaceQueryPartsHint of(
      String toBeReplaced1, String replacement1,
      String toBeReplaced2, String replacement2,
      String toBeReplaced3, String replacement3) {
    return new SpannerReplaceQueryPartsHint(
        ImmutableMap.of(toBeReplaced1, replacement1, toBeReplaced2, replacement2, toBeReplaced3,
            replacement3));
  }

  /**
   * Creates a hint that will replace all occurrences of {@code toBeReplaced1} with
   * {@code replacement1} and so on.
   */
  public static SpannerReplaceQueryPartsHint of(
      String toBeReplaced1, String replacement1,
      String toBeReplaced2, String replacement2,
      String toBeReplaced3, String replacement3,
      String toBeReplaced4, String replacement4) {
    return new SpannerReplaceQueryPartsHint(
        ImmutableMap.of(toBeReplaced1, replacement1, toBeReplaced2, replacement2, toBeReplaced3,
            replacement3, toBeReplaced4, replacement4));
  }

  private SpannerReplaceQueryPartsHint(ImmutableMap<String, String> replacements) {
    this.replacements = replacements;
  }

  /**
   * Creates a hint that will replace all occurrences of the keys in the given map with the
   * corresponding values in the map.
   */
  public SpannerReplaceQueryPartsHint(Map<String, String> replacements) {
    this.replacements = ImmutableMap.copyOf(replacements);
  }

  public SpannerReplaceQueryPartsHint combine(SpannerReplaceQueryPartsHint add) {
    return new SpannerReplaceQueryPartsHint(
        ImmutableMap.<String, String>builder()
            .putAll(this.replacements)
            .putAll(add.replacements)
            .build());
  }

  /** Applies the replacements to the given SQL statement. */
  String replace(String sql) {
    for (Entry<String, String> replacement : replacements.entrySet()) {
      sql = sql.replaceAll(replacement.getKey(), replacement.getValue());
    }
    return sql;
  }

  @Override
  public int hashCode() {
    return replacements.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SpannerReplaceQueryPartsHint)) {
      return false;
    }
    SpannerReplaceQueryPartsHint other = (SpannerReplaceQueryPartsHint) o;
    return Objects.equals(this.replacements, other.replacements);
  }

  /**
   * Returns this hint as a comment that can be added to a query.
   */
  public String toComment() {
    return toString();
  }

  @Override
  public String toString() {
    try (StringWriter writer = new StringWriter(); JsonGenerator generator = GsonFactory.getDefaultInstance()
        .createJsonGenerator(writer)) {
      generator.enablePrettyPrint();
      generator.writeStartObject();
      generator.writeFieldName(PREFIX);
      generator.writeStartObject();
      for (Entry<String, String> replacement : replacements.entrySet()) {
        generator.writeFieldName(replacement.getKey());
        generator.writeString(replacement.getValue());
      }
      generator.writeEndObject();
      generator.writeEndObject();
      return writer.toString();
    } catch (IOException ioException) {
      throw new HibernateException("failed to convert hint to comment: " + ioException.getMessage(),
          ioException);
    }
  }

  private static SpannerReplaceQueryPartsHint fromJson(String json) {
    try {
      JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
      JsonElement replacementsElement = jsonObject.get(PREFIX);
      if (replacementsElement == null) {
        return null;
      }
      JsonObject replacementsObject = replacementsElement.getAsJsonObject();
      ImmutableMap<String, String> replacementsMap = ImmutableMap.copyOf(
          replacementsObject.asMap().entrySet().stream()
              .map(entry -> MapEntry.of(entry.getKey(), entry.getValue().getAsString()))
              .collect(Collectors.toList()));
      return new SpannerReplaceQueryPartsHint(replacementsMap);
    } catch (JsonSyntaxException jsonSyntaxException) {
      throw new HibernateException("comment is invalid json: " + jsonSyntaxException.getMessage(),
          jsonSyntaxException);
    }
  }

  private static class MapEntry<K, V> implements Map.Entry<K, V> {

    private final K key;
    private final V value;

    static <K, V> MapEntry<K, V> of(K key, V value) {
      return new MapEntry<>(key, value);
    }

    private MapEntry(K key, V value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public V setValue(V value) {
      throw new UnsupportedOperationException();
    }
  }

}
