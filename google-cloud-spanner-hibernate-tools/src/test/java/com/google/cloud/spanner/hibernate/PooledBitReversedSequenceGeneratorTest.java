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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.spanner.hibernate.annotations.PooledBitReversedSequenceGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import jakarta.persistence.Id;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PooledBitReversedSequenceGeneratorTest {

  @Test
  public void testBuildGoogleSelect() {
    ServiceRegistry registry = mock(ServiceRegistry.class);
    JdbcEnvironment environment = mock(JdbcEnvironment.class);
    when(registry.getService(JdbcEnvironment.class)).thenReturn(environment);
    Dialect dialect = mock(Dialect.class);
    IdentifierHelper identifierHelper = mock(IdentifierHelper.class);
    when(environment.getDialect()).thenReturn(dialect);
    when(environment.getIdentifierHelper()).thenReturn(identifierHelper);
    when(identifierHelper.toIdentifier("")).thenReturn(Identifier.toIdentifier(""));
    when(identifierHelper.toIdentifier("")).thenReturn(Identifier.toIdentifier(""));
    when(identifierHelper.toIdentifier("test_sequence"))
        .thenReturn(Identifier.toIdentifier("test_sequence"));
    GeneratorCreationContext creationContext = mock(GeneratorCreationContext.class);
    when(creationContext.getServiceRegistry()).thenReturn(registry);
    when(creationContext.getType()).thenReturn(mock(Type.class));
    com.google.cloud.spanner.hibernate.PooledBitReversedSequenceGenerator generator =
        new com.google.cloud.spanner.hibernate.PooledBitReversedSequenceGenerator();
    generator.initialize(getAnnotation("id"), null, creationContext);
    assertEquals(
        "/* spanner.force_read_write_transaction=true */ "
            + "/* spanner.ignore_during_internal_retry=true */  "
            + "select get_next_sequence_value(sequence test_sequence) AS n "
            + "from unnest(generate_array(1, 5))",
        generator.getSelect());
  }

  @Test
  public void testBuildPostgresSelect() {
    ServiceRegistry registry = mock(ServiceRegistry.class);
    JdbcEnvironment environment = mock(JdbcEnvironment.class);
    when(registry.getService(JdbcEnvironment.class)).thenReturn(environment);
    Dialect dialect = new PostgreSQLDialect();
    IdentifierHelper identifierHelper = mock(IdentifierHelper.class);
    when(environment.getDialect()).thenReturn(dialect);
    when(environment.getIdentifierHelper()).thenReturn(identifierHelper);
    when(identifierHelper.toIdentifier("")).thenReturn(Identifier.toIdentifier(""));
    when(identifierHelper.toIdentifier("public")).thenReturn(Identifier.toIdentifier("public"));
    when(identifierHelper.toIdentifier("test_sequence"))
        .thenReturn(Identifier.toIdentifier("test_sequence"));
    GeneratorCreationContext creationContext = mock(GeneratorCreationContext.class);
    when(creationContext.getServiceRegistry()).thenReturn(registry);
    when(creationContext.getType()).thenReturn(mock(Type.class));
    com.google.cloud.spanner.hibernate.PooledBitReversedSequenceGenerator generator =
        new com.google.cloud.spanner.hibernate.PooledBitReversedSequenceGenerator();
    generator.initialize(getAnnotation("idPostgres"), null, creationContext);
    assertEquals(
        "/* spanner.force_read_write_transaction=true */ "
            + "/* spanner.ignore_during_internal_retry=true */  select "
            + "nextval('test_sequence') as n, nextval('test_sequence') as n, "
            + "nextval('test_sequence') as n, nextval('test_sequence') as n, "
            + "nextval('test_sequence') as n",
        generator.getSelect());
  }

  @Test
  public void parseExcludedRanges_empty_returnsEmptyList() {
    PooledBitReversedSequenceGenerator cfg = cfg("");
    List<Range<Long>> ranges =
        com.google.cloud.spanner.hibernate.PooledBitReversedSequenceGenerator.parseExcludedRanges(
            cfg);
    assertEquals(ImmutableList.of(), ranges);
  }

  @Test
  public void parseExcludedRanges_singleClosedRange_returnsRange() {
    PooledBitReversedSequenceGenerator cfg = cfg("[1,1000]");
    List<Range<Long>> ranges =
        com.google.cloud.spanner.hibernate.PooledBitReversedSequenceGenerator.parseExcludedRanges(
            cfg);
    assertEquals(ImmutableList.of(Range.closed(1L, 1000L)), ranges);
  }

  @Test
  public void parseExcludedRanges_negativeRange_returnsRange() {
    PooledBitReversedSequenceGenerator cfg = cfg("[-2000,-1000]");
    List<Range<Long>> ranges =
        com.google.cloud.spanner.hibernate.PooledBitReversedSequenceGenerator.parseExcludedRanges(
            cfg);
    assertEquals(ImmutableList.of(Range.closed(-2000L, -1000L)), ranges);
  }

  @Test
  public void parseExcludedRanges_missingBrackets_throwsMappingException() {
    PooledBitReversedSequenceGenerator cfg = cfg("1,1000");
    MappingException ex =
        assertThrows(
            MappingException.class,
            () ->
                com.google.cloud.spanner.hibernate.PooledBitReversedSequenceGenerator
                    .parseExcludedRanges(cfg));
    assertTrue(
        ex.getMessage(),
        ex.getMessage().contains("Range is not enclosed between '[' and ']'")
            && ex.getMessage().contains("Found '1,1000'"));
  }

  @Test
  public void parseExcludedRanges_notTwoElements_throwsMappingException() {
    // Two ranges in one string are not supported by this parser; it must be exactly one range.
    PooledBitReversedSequenceGenerator cfg = cfg("[1,10] [20,30]");
    MappingException ex =
        assertThrows(
            MappingException.class,
            () ->
                com.google.cloud.spanner.hibernate.PooledBitReversedSequenceGenerator
                    .parseExcludedRanges(cfg));
    assertTrue(
        ex.getMessage(),
        ex.getMessage().contains("Range does not contain exactly two elements")
            && ex.getMessage().contains("Found '[1,10] [20,30]'"));
  }

  @Test
  public void parseExcludedRanges_emptyStart_throwsMappingException() {
    PooledBitReversedSequenceGenerator cfg = cfg("[,1000]");
    MappingException ex =
        assertThrows(
            MappingException.class,
            () ->
                com.google.cloud.spanner.hibernate.PooledBitReversedSequenceGenerator
                    .parseExcludedRanges(cfg));
    assertTrue(
        ex.getMessage(),
        ex.getMessage().contains("For input string: \"\"")
            && ex.getMessage().contains("Found '[,1000]'"));
  }

  @Test
  public void parseExcludedRanges_nonNumeric_throwsMappingException() {
    PooledBitReversedSequenceGenerator cfg = cfg("[foo,1000]");
    MappingException ex =
        assertThrows(
            MappingException.class,
            () ->
                com.google.cloud.spanner.hibernate.PooledBitReversedSequenceGenerator
                    .parseExcludedRanges(cfg));
    assertTrue(
        ex.getMessage(),
        ex.getMessage().contains("For input string: \"foo\"")
            && ex.getMessage().contains("Found '[foo,1000]'"));
  }

  private static PooledBitReversedSequenceGenerator cfg(String excludeRange) {
    return new PooledBitReversedSequenceGenerator() {
      @Override
      public String sequenceName() {
        return "test_sequence";
      }

      @Override
      public int startWithCounter() {
        return 1;
      }

      @Override
      public int poolSize() {
        return 50;
      }

      @Override
      public String excludeRange() {
        return excludeRange;
      }

      @Override
      public String schema() {
        return "";
      }

      @Override
      public Class<? extends Annotation> annotationType() {
        return com.google.cloud.spanner.hibernate.annotations.PooledBitReversedSequenceGenerator
            .class;
      }
    };
  }

  /** Test entity used to attach different annotation configurations for tests. */
  private static class TestEntity {
    @Id
    @PooledBitReversedSequenceGenerator(sequenceName = "test_sequence", poolSize = 5)
    Long id;

    @Id
    @PooledBitReversedSequenceGenerator(sequenceName = "test_sequence", poolSize = 5)
    Long idPostgres;
  }

  private static PooledBitReversedSequenceGenerator getAnnotation(String fieldName) {
    try {
      Field f = TestEntity.class.getDeclaredField(fieldName);
      return f.getAnnotation(PooledBitReversedSequenceGenerator.class);
    } catch (NoSuchFieldException e) {
      throw new AssertionError("Failed to get annotation", e);
    }
  }
}
