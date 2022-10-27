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
import static org.mockito.Mockito.mock;

import com.google.cloud.spanner.hibernate.entities.Customer;
import java.io.Serializable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
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
}
