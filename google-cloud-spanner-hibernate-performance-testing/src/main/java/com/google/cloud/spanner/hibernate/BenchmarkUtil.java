/*
 * Copyright 2019 Google LLC
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

import org.apache.log4j.Logger;

/**
 * Utility methods for benchmarking code.
 */
public class BenchmarkUtil {

  private static final Logger LOGGER = Logger.getLogger(BenchmarkUtil.class);

  /**
   * Utility method which executes a {@link Runnable} and returns the amount of time it took to run.
   * @param runnable The action to run.
   * @return The time in milliseconds for the operation to run.
   */
  public static long benchmark(Runnable runnable) {
    long startMs = System.currentTimeMillis();
    try {
      runnable.run();
    } catch (Exception e) {
      throw new RuntimeException("Failed to benchmark code.", e);
    }
    return System.currentTimeMillis() - startMs;
  }

  /**
   * Utility method which executes a {@link Runnable}, returns the time taken, and logs the
   * operation with the provided {@code description}.
   * @param runnable The action to run.
   * @param description The description of the action.
   * @return The time in milliseconds for the operation to run.
   */
  public static long benchmark(Runnable runnable, String description) {
    LOGGER.info(description);

    long timeTaken = benchmark(runnable);
    LOGGER.info("Milliseconds taken: " + timeTaken);
    return timeTaken;
  }
}
