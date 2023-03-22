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

import static com.google.cloud.spanner.hibernate.BitReversedSequenceStyleGenerator.EXCLUDE_RANGES_PARAM;
import static com.google.cloud.spanner.hibernate.BitReversedSequenceStyleGenerator.parseExcludedRanges;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.spanner.AbortedDueToConcurrentModificationException;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.hibernate.entities.Customer;
import com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory.JdbcAbortedDueToConcurrentModificationException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.id.IdentifierGenerationException;
import org.junit.Test;

/** Tests for {@link BitReversedSequenceStyleGenerator}. */
public class BitReversedSequenceStyleGeneratorTests {

  static Properties asProperties(Map<String, String> map) {
    Properties properties = new Properties();
    for (Entry<String, String> entry : map.entrySet()) {
      properties.setProperty(entry.getKey(), entry.getValue());
    }
    return properties;
  }

  @Test
  public void testIsBitReversed() {
    SharedSessionContractImplementor session = mock(SharedSessionContractImplementor.class);
    Customer customer = new Customer();
    BitReversedSequenceStyleGenerator generator =
        new BitReversedSequenceStyleGenerator() {
          protected Serializable generateBaseValue(
              SharedSessionContractImplementor session, Object entity) {
            return 0b1111111111111111111111111110000000000000000000000000000010010001L;
          }
        };
    assertEquals(
        0b1000100100000000000000000000000000000111111111111111111111111111L,
        generator.generate(session, customer));
  }

  @Test
  public void testIntegersAreNotBitReversed() {
    SharedSessionContractImplementor session = mock(SharedSessionContractImplementor.class);
    Customer customer = new Customer();
    BitReversedSequenceStyleGenerator generator =
        new BitReversedSequenceStyleGenerator() {
          protected Serializable generateBaseValue(
              SharedSessionContractImplementor session, Object entity) {
            return 100;
          }
        };
    assertEquals(100, generator.generate(session, customer));
  }

  @Test
  public void testStringsAreNotBitReversed() {
    SharedSessionContractImplementor session = mock(SharedSessionContractImplementor.class);
    Customer customer = new Customer();
    String id = UUID.randomUUID().toString();
    BitReversedSequenceStyleGenerator generator =
        new BitReversedSequenceStyleGenerator() {
          protected Serializable generateBaseValue(
              SharedSessionContractImplementor session, Object entity) {
            return id;
          }
        };
    assertEquals(id, generator.generate(session, customer));
  }

  @Test
  public void testGenerateRetriesAbortedException() {
    JdbcAbortedDueToConcurrentModificationException jdbcAbortedException =
        mock(JdbcAbortedDueToConcurrentModificationException.class);
    AbortedDueToConcurrentModificationException abortedException =
        mock(AbortedDueToConcurrentModificationException.class);
    when(jdbcAbortedException.getCause()).thenReturn(abortedException);
    SharedSessionContractImplementor session = mock(SharedSessionContractImplementor.class);
    Customer customer = new Customer();
    AtomicInteger attempt = new AtomicInteger();
    BitReversedSequenceStyleGenerator generator =
        new BitReversedSequenceStyleGenerator() {
          protected Serializable generateBaseValue(
              SharedSessionContractImplementor session, Object entity) {
            if (attempt.addAndGet(1) == 1) {
              throw new GenericJDBCException("Transaction was aborted", jdbcAbortedException);
            } else {
              return 1L;
            }
          }
        };
    assertEquals(Long.reverse(1L), generator.generate(session, customer));
    assertEquals(2, attempt.get());
  }

  @Test
  public void testGeneratePropagatesInterruptedExceptionDuringRetry() {
    JdbcAbortedDueToConcurrentModificationException jdbcAbortedException =
        mock(JdbcAbortedDueToConcurrentModificationException.class);
    AbortedDueToConcurrentModificationException abortedException =
        mock(AbortedDueToConcurrentModificationException.class);
    when(jdbcAbortedException.getCause()).thenReturn(abortedException);
    SharedSessionContractImplementor session = mock(SharedSessionContractImplementor.class);
    Customer customer = new Customer();
    BitReversedSequenceStyleGenerator generator =
        new BitReversedSequenceStyleGenerator() {
          @Override
          protected void sleep(long millis) throws InterruptedException {
            throw new InterruptedException();
          }

          protected Serializable generateBaseValue(
              SharedSessionContractImplementor session, Object entity) {
            throw new GenericJDBCException("Transaction was aborted", jdbcAbortedException);
          }
        };
    IdentifierGenerationException exception =
        assertThrows(
            IdentifierGenerationException.class, () -> generator.generate(session, customer));
    assertEquals("Interrupted while trying to generate a new ID", exception.getMessage());
    assertTrue(Thread.interrupted());
  }

  @Test
  public void testGeneratePropagatesOtherExceptions() {
    SharedSessionContractImplementor session = mock(SharedSessionContractImplementor.class);
    Customer customer = new Customer();
    BitReversedSequenceStyleGenerator generator =
        new BitReversedSequenceStyleGenerator() {
          protected Serializable generateBaseValue(
              SharedSessionContractImplementor session, Object entity) {
            throw mock(SpannerException.class);
          }
        };
    assertThrows(SpannerException.class, () -> generator.generate(session, customer));
  }

  @Test
  public void testGenerateGivesUpEventually() {
    JdbcAbortedDueToConcurrentModificationException jdbcAbortedException =
        mock(JdbcAbortedDueToConcurrentModificationException.class);
    AbortedDueToConcurrentModificationException abortedException =
        mock(AbortedDueToConcurrentModificationException.class);
    when(jdbcAbortedException.getCause()).thenReturn(abortedException);
    SharedSessionContractImplementor session = mock(SharedSessionContractImplementor.class);
    Customer customer = new Customer();
    AtomicInteger attempts = new AtomicInteger();
    BitReversedSequenceStyleGenerator generator =
        new BitReversedSequenceStyleGenerator() {
          protected Serializable generateBaseValue(
              SharedSessionContractImplementor session, Object entity) {
            attempts.incrementAndGet();
            throw new GenericJDBCException("Transaction was aborted", jdbcAbortedException);
          }
        };
    assertThrows(GenericJDBCException.class, () -> generator.generate(session, customer));
    assertEquals(BitReversedSequenceStyleGenerator.MAX_ATTEMPTS, attempts.get());
  }

  @Test
  public void testGenerateSkipsExcludedRanges() {
    SharedSessionContractImplementor session = mock(SharedSessionContractImplementor.class);
    Customer customer = new Customer();
    Properties params = new Properties();
    long bitReversed1 = Long.reverse(1L);
    long bitReversed2 = Long.reverse(2L);
    params.setProperty(
        EXCLUDE_RANGES_PARAM,
        String.format("[%d,%d] [%d,%d]", bitReversed1, bitReversed1, bitReversed2, bitReversed2));
    AtomicLong counter = new AtomicLong();
    BitReversedSequenceStyleGenerator generator =
        new BitReversedSequenceStyleGenerator() {
          protected Serializable generateBaseValue(
              SharedSessionContractImplementor session, Object entity) {
            return counter.incrementAndGet();
          }
        };
    generator.configureExcludedRanges("test_sequence", params);
    assertEquals(Long.reverse(3L), generator.generate(session, customer));
  }

  @Test
  public void testParseExcludedRanges() {
    assertEquals(ImmutableList.of(), parseExcludedRanges("test_sequence", new Properties()));
    assertEquals(
        ImmutableList.of(Range.closed(1L, 1L)),
        parseExcludedRanges(
            "test_sequence", asProperties(ImmutableMap.of(EXCLUDE_RANGES_PARAM, "[1,1]"))));
    assertEquals(
        ImmutableList.of(Range.closed(1L, 1000L)),
        parseExcludedRanges(
            "test_sequence", asProperties(ImmutableMap.of(EXCLUDE_RANGES_PARAM, "[1,1000]"))));
    assertEquals(
        ImmutableList.of(Range.closed(-2000L, -1000L)),
        parseExcludedRanges(
            "test_sequence", asProperties(ImmutableMap.of(EXCLUDE_RANGES_PARAM, "[-2000,-1000]"))));
    assertEquals(
        ImmutableList.of(Range.closed(1L, 10L), Range.closed(20L, 30L)),
        parseExcludedRanges(
            "test_sequence",
            asProperties(ImmutableMap.of(EXCLUDE_RANGES_PARAM, "[1,10] [20,30]"))));
    assertEquals(
        ImmutableList.of(Range.closed(1L, 10L), Range.closed(20L, 30L), Range.closed(-30L, -20L)),
        parseExcludedRanges(
            "test_sequence",
            asProperties(ImmutableMap.of(EXCLUDE_RANGES_PARAM, "[1,10] [20,30] [-30,-20]"))));

    assertEquals(
        "Invalid range found for the [test_sequence] sequence: For input string: \"foo\"\n"
            + "Excluded ranges must be given as a space-separated sequence of ranges between "
            + "square brackets, e.g. '[1,1000] [2001,3000]'. Found '[foo,-2000]'",
        assertThrows(
                MappingException.class,
                () ->
                    parseExcludedRanges(
                        "test_sequence",
                        asProperties(ImmutableMap.of(EXCLUDE_RANGES_PARAM, "[foo,-2000]"))))
            .getMessage());
    assertEquals(
        "Invalid range found for the [test_sequence] sequence: For input string: \"\"\n"
            + "Excluded ranges must be given as a space-separated sequence of ranges between "
            + "square brackets, e.g. '[1,1000] [2001,3000]'. Found '[,1000]'",
        assertThrows(
                MappingException.class,
                () ->
                    parseExcludedRanges(
                        "test_sequence",
                        asProperties(ImmutableMap.of(EXCLUDE_RANGES_PARAM, "[,1000]"))))
            .getMessage());
    assertEquals(
        "Invalid range found for the [test_sequence] sequence: "
            + "Range does not contain exactly two elements\n"
            + "Excluded ranges must be given as a space-separated sequence of ranges between "
            + "square brackets, e.g. '[1,1000] [2001,3000]'. Found '[1,1000][2000,3000]'",
        assertThrows(
                MappingException.class,
                () ->
                    parseExcludedRanges(
                        "test_sequence",
                        asProperties(ImmutableMap.of(EXCLUDE_RANGES_PARAM, "[1,1000][2000,3000]"))))
            .getMessage());
    assertEquals(
        "Invalid range found for the [test_sequence] sequence: "
            + "Range is not enclosed between '[' and ']'\n"
            + "Excluded ranges must be given as a space-separated sequence of ranges between "
            + "square brackets, e.g. '[1,1000] [2001,3000]'. Found '1,1000'",
        assertThrows(
                MappingException.class,
                () ->
                    parseExcludedRanges(
                        "test_sequence",
                        asProperties(ImmutableMap.of(EXCLUDE_RANGES_PARAM, "1,1000 2000,3000"))))
            .getMessage());
    assertEquals(
        "Invalid range found for the [test_sequence] sequence: Invalid range: [-1000..-2000]\n"
            + "Excluded ranges must be given as a space-separated sequence of ranges "
            + "between square brackets, e.g. '[1,1000] [2001,3000]'. Found '[-1000,-2000]'",
        assertThrows(
                MappingException.class,
                () ->
                    parseExcludedRanges(
                        "test_sequence",
                        asProperties(ImmutableMap.of(EXCLUDE_RANGES_PARAM, "[-1000,-2000]"))))
            .getMessage());
  }
}
