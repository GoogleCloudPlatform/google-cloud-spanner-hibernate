package com.google.cloud.spanner.hibernate;

import org.apache.log4j.Logger;

public class BenchmarkUtil {

  private static final Logger LOGGER = Logger.getLogger(BenchmarkUtil.class);

  public static void benchmark(Runnable runnable, String description) {
    LOGGER.info(description);
    long startMs = System.currentTimeMillis();
    try {
      runnable.run();
    } catch (Exception e) {
      throw new RuntimeException("Failed to benchmark code: " + description, e);
    }
    LOGGER.info("Milliseconds taken: " + (System.currentTimeMillis() - startMs));
  }
}
