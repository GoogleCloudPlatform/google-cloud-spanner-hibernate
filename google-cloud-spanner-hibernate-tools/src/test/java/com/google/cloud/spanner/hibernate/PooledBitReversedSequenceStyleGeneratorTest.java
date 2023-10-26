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

import static com.google.cloud.spanner.hibernate.PooledBitReversedSequenceStyleGenerator.EXCLUDE_RANGE_PARAM;
import static com.google.cloud.spanner.hibernate.PooledBitReversedSequenceStyleGenerator.parseExcludedRanges;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for bit-reversed sequence generator. */
@RunWith(JUnit4.class)
public class PooledBitReversedSequenceStyleGeneratorTest {

  @Test
  public void testParseExcludedRanges() {
    assertEquals(ImmutableList.of(), parseExcludedRanges("test_sequence", new Properties()));
    assertEquals(
        ImmutableList.of(Range.closed(1L, 1L)),
        parseExcludedRanges(
            "test_sequence", asProperties(ImmutableMap.of(EXCLUDE_RANGE_PARAM, "[1,1]"))));
    assertEquals(
        ImmutableList.of(Range.closed(1L, 1000L)),
        parseExcludedRanges(
            "test_sequence", asProperties(ImmutableMap.of(EXCLUDE_RANGE_PARAM, "[1,1000]"))));
    assertEquals(
        ImmutableList.of(Range.closed(-2000L, -1000L)),
        parseExcludedRanges(
            "test_sequence", asProperties(ImmutableMap.of(EXCLUDE_RANGE_PARAM, "[-2000,-1000]"))));
    assertEquals(
        ImmutableList.of(Range.closed(1L, 10L), Range.closed(20L, 30L)),
        parseExcludedRanges(
            "test_sequence",
            asProperties(ImmutableMap.of(EXCLUDE_RANGE_PARAM, "[1,10] [20,30]"))));
    assertEquals(
        ImmutableList.of(Range.closed(1L, 10L), Range.closed(20L, 30L), Range.closed(-30L, -20L)),
        parseExcludedRanges(
            "test_sequence",
            asProperties(ImmutableMap.of(EXCLUDE_RANGE_PARAM, "[1,10] [20,30] [-30,-20]"))));

    assertEquals(
        "Invalid range found for the [test_sequence] sequence: For input string: \"foo\"\n"
            + "Excluded ranges must be given as a space-separated sequence of ranges between "
            + "square brackets, e.g. '[1,1000] [2001,3000]'. Found '[foo,-2000]'",
        assertThrows(
            MappingException.class,
            () ->
                parseExcludedRanges(
                    "test_sequence",
                    asProperties(ImmutableMap.of(EXCLUDE_RANGE_PARAM, "[foo,-2000]"))))
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
                    asProperties(ImmutableMap.of(EXCLUDE_RANGE_PARAM, "[,1000]"))))
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
                    asProperties(ImmutableMap.of(EXCLUDE_RANGE_PARAM, "[1,1000][2000,3000]"))))
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
                    asProperties(ImmutableMap.of(EXCLUDE_RANGE_PARAM, "1,1000 2000,3000"))))
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
                    asProperties(ImmutableMap.of(EXCLUDE_RANGE_PARAM, "[-1000,-2000]"))))
            .getMessage());
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
    when(identifierHelper.toIdentifier("public"))
        .thenReturn(Identifier.toIdentifier("public"));
    when(identifierHelper.toIdentifier("test_sequence"))
        .thenReturn(Identifier.toIdentifier("test_sequence"));
    
    PooledBitReversedSequenceStyleGenerator generator
        = new PooledBitReversedSequenceStyleGenerator();
    generator.configure(mock(Type.class), asProperties(ImmutableMap.of(
        "catalog", "",
        "schema", "public",
        "sequence_name", "test_sequence",
        "increment_size", "5"
    )), registry);
    assertEquals("/* spanner.force_read_write_transaction=true */ " 
        + "/* spanner.ignore_during_internal_retry=true */  select " 
        + "nextval('test_sequence') as n, nextval('test_sequence') as n, " 
        + "nextval('test_sequence') as n, nextval('test_sequence') as n, " 
        + "nextval('test_sequence') as n", generator.getSelect());
  }

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

    PooledBitReversedSequenceStyleGenerator generator =
        new PooledBitReversedSequenceStyleGenerator();
    generator.configure(mock(Type.class), asProperties(ImmutableMap.of(
        "catalog", "",
        "schema", "public",
        "sequence_name", "test_sequence",
        "increment_size", "5"
    )), registry);
    assertEquals("/* spanner.force_read_write_transaction=true */ " 
        + "/* spanner.ignore_during_internal_retry=true */  " 
        + "select get_next_sequence_value(sequence test_sequence) AS n " 
        + "from unnest(generate_array(1, 5))", generator.getSelect());
  }
  
  static Properties asProperties(Map<String, String> map) {
    Properties properties = new Properties();
    for (Entry<String, String> entry : map.entrySet()) {
      properties.setProperty(entry.getKey(), entry.getValue());
    }
    return properties;
  }

}
