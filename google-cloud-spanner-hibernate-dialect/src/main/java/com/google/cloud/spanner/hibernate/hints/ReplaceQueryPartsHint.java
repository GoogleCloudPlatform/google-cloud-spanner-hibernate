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

import com.google.api.client.json.JsonGenerator;
import com.google.api.client.json.gson.GsonFactory;
import com.google.cloud.spanner.hibernate.SpannerDialect;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Objects;
import org.hibernate.HibernateException;

/**
 * Hint that can be used in combination with the {@link SpannerDialect} to replace specific parts of
 * a generated query. This can for example be used to replace a `FROM my_table` clause with one that
 * uses an index hint: `FROM my_table@{FORCE_INDEX=my_index}`.
 *
 * <p>The hint must be a JSON string in the following format:
 *
 * <pre>{@code
 * {
 *   spanner_replacements: [
 *     {
 *       regex: "string to replace",
 *       replacement: "replacement string",
 *       replace_mode: "ALL" | "FIRST"
 *     },
 *     ...
 *   ]
 * }
 * }</pre>
 *
 * <p>Example:
 *
 * <pre>{@code
 * {
 *   spanner_replacements: [
 *     {
 *       regex: " from singers ",
 *       replacement: " from singers@{force_index=idx_singers_last_name} ",
 *       replace_mode: "ALL"
 *     },
 *     {
 *       regex: " join albums ",
 *       replacement: " join albums@{force_index=idx_albums_title} ",
 *       replace_mode: "FIRST"
 *     }
 *   ]
 * }
 * }</pre>
 */
public class ReplaceQueryPartsHint {

  /** Specifies how replacements should be applied to the query string. */
  public enum ReplaceMode {
    /** Only replace the first occurrence in the query string. */
    FIRST {
      @Override
      String apply(String sql, String key, String replacement) {
        return sql.replaceFirst(key, replacement);
      }
    },

    /** Replace all occurrences in the query string (default). */
    ALL {
      @Override
      String apply(String sql, String key, String replacement) {
        return sql.replaceAll(key, replacement);
      }
    };

    abstract String apply(String sql, String key, String replacement);
  }

  /** Replacement that should be applied to a query string. */
  public static class Replacement {
    private final String regex;

    private final String replacement;

    private final ReplaceMode replaceMode;

    /**
     * Creates a query part replacement.
     *
     * @param regex a regular expression for the part of the query that should be replaced
     * @param replacement the replacement string that should replace <code>regex</code>. May contain
     *     $1, $2, ... to refer to matching groups in the regular expression.
     * @param replaceMode whether to replace all or only the first occurrence in the query string
     */
    public Replacement(String regex, String replacement, ReplaceMode replaceMode) {
      this.regex = Preconditions.checkNotNull(regex);
      this.replacement = Preconditions.checkNotNull(replacement);
      this.replaceMode = Preconditions.checkNotNull(replaceMode);
    }

    @Override
    public int hashCode() {
      return Objects.hash(regex, replacement, replaceMode);
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Replacement)) {
        return false;
      }
      Replacement other = (Replacement) o;
      return Objects.equals(this.regex, other.regex)
          && Objects.equals(this.replacement, other.replacement)
          && Objects.equals(this.replaceMode, other.replaceMode);
    }
  }

  public static final String SPANNER_REPLACEMENTS_FIELD_NAME = "spanner_replacements";

  static final String REGEX_FIELD_NAME = "regex";

  static final String REPLACEMENT_FIELD_NAME = "replacement";

  static final String REPLACE_MODE_FIELD_NAME = "replace_mode";

  private final List<Replacement> replacements;

  /** Re-creates a hint from a JSON-formatted comment. */
  public static ReplaceQueryPartsHint fromComment(String comment) {
    return fromJson(comment);
  }

  /** Creates a hint that will replace all occurrences of {@code regex} with {@code replacement}. */
  public static ReplaceQueryPartsHint of(String regex, String replacement) {
    return new ReplaceQueryPartsHint(
        ImmutableList.of(new Replacement(regex, replacement, ReplaceMode.ALL)));
  }

  /**
   * Creates a hint that will replace occurrences of {@code regex} with {@code replacement} using
   * the given {@link ReplaceMode}.
   */
  public static ReplaceQueryPartsHint of(
      String regex, String replacement, ReplaceMode replaceMode) {
    return new ReplaceQueryPartsHint(
        ImmutableList.of(new Replacement(regex, replacement, replaceMode)));
  }

  protected ReplaceQueryPartsHint(ImmutableList<Replacement> replacements) {
    this.replacements = Preconditions.checkNotNull(replacements);
  }

  /** Creates a hint that will apply all the replacements in the given list. */
  public ReplaceQueryPartsHint(List<Replacement> replacements) {
    this.replacements = ImmutableList.copyOf(Preconditions.checkNotNull(replacements));
  }

  /**
   * Creates a new combined hint of this and the other hint. If both hints contain a replacement for
   * a given substring, then the replacement value of <code>other</code> will be used.
   */
  public ReplaceQueryPartsHint combine(ReplaceQueryPartsHint other) {
    return new ReplaceQueryPartsHint(
        ImmutableList.<Replacement>builder()
            .addAll(this.replacements)
            .addAll(other.replacements)
            .build());
  }

  /** Applies the replacements to the given SQL statement. */
  public String replace(String sql) {
    for (Replacement replacement : replacements) {
      sql = replacement.replaceMode.apply(sql, replacement.regex, replacement.replacement);
    }
    return sql;
  }

  @Override
  public int hashCode() {
    return replacements.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ReplaceQueryPartsHint)) {
      return false;
    }
    ReplaceQueryPartsHint other = (ReplaceQueryPartsHint) o;
    return Objects.equals(this.replacements, other.replacements);
  }

  /** Returns this hint as a query hint that can be added to a query. */
  public String toQueryHint() {
    return toString();
  }

  /** Returns this hint as a comment that can be added to a query. */
  public String toComment() {
    return toString();
  }

  @Override
  public String toString() {
    try (StringWriter writer = new StringWriter();
        JsonGenerator generator = GsonFactory.getDefaultInstance().createJsonGenerator(writer)) {
      generator.enablePrettyPrint();
      generator.writeStartObject();
      generator.writeFieldName(SPANNER_REPLACEMENTS_FIELD_NAME);
      generator.writeStartArray();
      for (Replacement replacement : replacements) {
        generator.writeStartObject();
        generator.writeFieldName(REGEX_FIELD_NAME);
        generator.writeString(replacement.regex);
        generator.writeFieldName(REPLACEMENT_FIELD_NAME);
        generator.writeString(replacement.replacement);
        generator.writeFieldName(REPLACE_MODE_FIELD_NAME);
        generator.writeString(replacement.replaceMode.name());
        generator.writeEndObject();
      }
      generator.writeEndArray();
      generator.writeEndObject();
      return writer.toString();
    } catch (IOException ioException) {
      throw new HibernateException(
          "failed to convert hint to comment: " + ioException.getMessage(), ioException);
    }
  }

  /** Re-creates a hint from a JSON-formatted string. */
  public static ReplaceQueryPartsHint fromJson(String json) {
    try {
      JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
      JsonElement replacementsElement = jsonObject.get(SPANNER_REPLACEMENTS_FIELD_NAME);
      if (replacementsElement == null) {
        throw new HibernateException(
            "Hint does not contain a " + SPANNER_REPLACEMENTS_FIELD_NAME + " element at the root");
      }
      if (!replacementsElement.isJsonArray()) {
        throw new HibernateException(SPANNER_REPLACEMENTS_FIELD_NAME + " must be an array");
      }
      JsonArray replacementsArray = replacementsElement.getAsJsonArray();
      ImmutableList.Builder<Replacement> replacementsMapBuilder = ImmutableList.builder();
      for (JsonElement replacement : replacementsArray.asList()) {
        if (!replacement.isJsonObject()) {
          throw new HibernateException(
              "All elements of " + SPANNER_REPLACEMENTS_FIELD_NAME + " must be objects");
        }
        JsonObject replacementObject = replacement.getAsJsonObject();
        JsonElement regexElement = replacementObject.get(REGEX_FIELD_NAME);
        if (regexElement == null) {
          throw new HibernateException(
              "Missing " + REGEX_FIELD_NAME + " field in replacement object");
        }
        if (!regexElement.isJsonPrimitive()) {
          throw new HibernateException(REGEX_FIELD_NAME + " must be a string");
        }
        String regex = getAsString(replacementObject, REGEX_FIELD_NAME);
        String replacementString = getAsString(replacementObject, REPLACEMENT_FIELD_NAME);
        String replaceModeString =
            getAsString(replacementObject, REPLACE_MODE_FIELD_NAME, /* optional = */ true);
        ReplaceMode replaceMode =
            replaceModeString == null ? ReplaceMode.ALL : ReplaceMode.valueOf(replaceModeString);
        replacementsMapBuilder.add(new Replacement(regex, replacementString, replaceMode));
      }
      return new ReplaceQueryPartsHint(replacementsMapBuilder.build());
    } catch (JsonSyntaxException exception) {
      throw new HibernateException(
          "Comment is not a valid hint: " + exception.getMessage(), exception);
    }
  }

  private static String getAsString(JsonObject jsonObject, String fieldName) {
    return getAsString(jsonObject, fieldName, false);
  }

  private static String getAsString(JsonObject jsonObject, String fieldName, boolean optional) {
    JsonElement element = jsonObject.get(fieldName);
    if (element == null) {
      if (optional) {
        return null;
      }
      throw new HibernateException("Missing " + fieldName + " field in replacement object");
    }
    if (!element.isJsonPrimitive()) {
      throw new HibernateException(fieldName + " must be a string");
    }
    return element.getAsString();
  }
}
