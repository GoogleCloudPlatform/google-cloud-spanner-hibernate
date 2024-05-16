/*
 * Copyright 2019-2023 Google LLC
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

import static com.google.cloud.spanner.hibernate.SpannerDialect.SPANNER_DISABLE_SEQUENCES_PROPERTY;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.util.stream.Collectors;
import org.hibernate.MappingException;
import org.hibernate.dialect.sequence.SequenceSupport;

/** Sequence support for Cloud Spanner. */
public class SpannerSequenceSupport implements SequenceSupport {

  @Override
  public String getCreateSequenceString(String sequenceName) throws MappingException {
    return getCreateSequenceString(sequenceName, 1, "");
  }

  @Override
  public String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize)
      throws MappingException {
    if (incrementSize == 1) {
      return getCreateSequenceString(sequenceName, initialValue, "");
    }
    throw new MappingException(
        "Cloud Spanner does not support sequences with an increment size != 1");
  }

  String getCreateSequenceString(String sequenceName, int initialValue, String additionalOptions) {
    ImmutableMap.Builder<String, String> options = ImmutableMap.builder();
    options.put("sequence_kind", "\"bit_reversed_positive\"");
    if (initialValue != 1) {
      options.put("start_with_counter", String.valueOf(initialValue));
    }
    if (!Strings.isNullOrEmpty(additionalOptions)) {
      additionalOptions = ", " + additionalOptions + ")";
    } else {
      additionalOptions = ")";
    }
    return "create sequence "
        + sequenceName
        + options.build().entrySet().stream()
            .map(option -> option.getKey() + "=" + option.getValue())
            .collect(Collectors.joining(", ", " options(", additionalOptions));
  }

  @Override
  public String getDropSequenceString(String sequenceName) {
    return "drop sequence " + sequenceName;
  }

  @Override
  public String getSequenceNextValString(String sequenceName) {
    // This statement includes a comment that is seen as a hint by the Cloud Spanner JDBC driver to
    // ignore this SELECT statement if the transaction is aborted and retried. Selecting from a
    // sequence always updates the sequence, regardless whether the transaction committed or not,
    // and retrying it is not necessary, and would always return different values.
    //
    // The statement also includes a comment that instructs the Cloud Spanner JDBC driver to always
    // start a read/write transaction, even though the statement is a SELECT statement.
    return "/* spanner.force_read_write_transaction=true */ "
        + "/* spanner.ignore_during_internal_retry=true */ "
        + "select "
        + getSelectSequenceNextValString(sequenceName);
  }

  @Override
  public String getSelectSequenceNextValString(String sequenceName) {
    return "get_next_sequence_value(sequence " + sequenceName + ")";
  }

  @Override
  public boolean supportsSequences() {
    // Sequences are enabled and supported by default, but can be turned off if it interferes with
    // existing table-backed sequential generators.
    String disableSequences = System.getProperty(SPANNER_DISABLE_SEQUENCES_PROPERTY, "false");
    try {
      return !Boolean.parseBoolean(disableSequences);
    } catch (Throwable ignore) {
      return true;
    }
  }

  @Override
  public boolean supportsPooledSequences() {
    // 'Pooled' sequences support an increment size > 1.
    return false;
  }
}
