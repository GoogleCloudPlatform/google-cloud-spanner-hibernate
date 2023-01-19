/*
 * Copyright 2019-2020 Google LLC
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.spanner.AbortedDueToConcurrentModificationException;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.hibernate.entities.Customer;
import com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory.JdbcAbortedDueToConcurrentModificationException;
import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.id.IdentifierGenerationException;
import org.junit.Test;

/** Tests for {@link BitReversedSequenceStyleGenerator}. */
public class BitReversedSequenceStyleGeneratorTests {

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
    assertEquals(
        100,
        generator.generate(session, customer));
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
    assertEquals(
        id,
        generator.generate(session, customer));
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
    assertEquals(
        Long.reverse(1L),
        generator.generate(session, customer));
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
    IdentifierGenerationException exception = assertThrows(
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
    assertThrows(GenericJDBCException.class,
        () -> generator.generate(session, customer));
    assertEquals(BitReversedSequenceStyleGenerator.MAX_ATTEMPTS, attempts.get());
  }

}
