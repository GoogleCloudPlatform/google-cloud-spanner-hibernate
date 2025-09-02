/*
 * Copyright 2019-2025 Google LLC
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
package com.google.cloud.spanner.hibernate.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.hibernate.annotations.IdGeneratorType;

/**
 * Pooled ID generator that uses a bit-reversed sequence to generate values. These values are safe
 * to use as the primary key of a table in Cloud Spanner. This is the recommended strategy for
 * auto-generated numeric primary keys in Cloud Spanner.
 *
 * <p>Using a bit-reversed sequence for ID generation is recommended over sequences that return a
 * monotonically increasing value in Cloud Spanner. This generator supports:
 *
 * <ul>
 *   <li>Batch fetching of IDs (pool size &gt; 1) to reduce round-trips.
 *   <li>An initial counter value &gt; 1 which is used for bit-reversing.
 *   <li>Excluding a contiguous range of values to avoid collisions with existing data.
 * </ul>
 *
 * <p>It is recommended to use a separate sequence per entity. Set the sequence name with {@link
 * #sequenceName()} and optionally a {@link #schema()} if the sequence is not fully qualified in
 * {@code sequenceName()}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Id
 * @PooledBitReversedSequenceGenerator(
 *     sequenceName = "customer_id_sequence",
 *     poolSize = 200,
 *     startWithCounter = 50000,
 *     excludeRange = "[1,1000]"
 * )
 * private Long id;
 * }</pre>
 */
@IdGeneratorType(com.google.cloud.spanner.hibernate.PooledBitReversedSequenceGenerator.class)
@Retention(RUNTIME)
@Target({METHOD, FIELD})
public @interface PooledBitReversedSequenceGenerator {

  /**
   * Name of the database sequence to use. May be fully qualified. If not, {@link #schema()} is
   * applied when present.
   */
  String sequenceName();

  /**
   * Positive starting counter value for the sequence (raw counter prior to bit-reversal). Must be
   * greater than zero.
   */
  int startWithCounter() default 1;

  /**
   * Number of IDs to fetch per batch. Must be greater than zero. Larger values reduce round-trips
   * but may increase the number of unused IDs on shutdown.
   */
  int poolSize() default 50;

  /**
   * Optional closed range of values to skip, formatted as "[from,to]". Example: "[1,1000]" to skip
   * 1 through 1000 inclusive.
   */
  String excludeRange() default "";

  /** Optional schema to resolve {@link #sequenceName()} when it is not fully qualified. */
  String schema() default "";
}
