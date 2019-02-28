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

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * The exporter for Cloud Spanner CREATE and DROP table statements.
 *
 * @author Chengyuan Zhao
 */
public class SpannerTableExporter implements Exporter<Table> {

  private final SpannerDialect spannerDialect;

  /**
   * Constructor.
   *
   * @param spannerDialect a Cloud Spanner dialect.
   */
  public SpannerTableExporter(SpannerDialect spannerDialect) {
    this.spannerDialect = spannerDialect;
  }

  @Override
  public String[] getSqlCreateStrings(Table table, Metadata metadata) {
    /* The current implementation does not support interleaved tables for collections
     * or UNIQUE constraints/indexes for relationships
     * */

    if (!table.hasPrimaryKey()) {
      throw new UnsupportedOperationException("Cloud Spanner requires tables and entities to have"
          + " at least one ID column to act as the Primary Key.");
    }

    String createTableTemplate = this.spannerDialect.getCreateTableString()
        + " {0} ({1}) PRIMARY KEY ({2})";

    String primaryKeyColNames = table.getPrimaryKey().getColumns()
        .stream()
        .map(Column::getQuotedName)
        .collect(Collectors.joining(","));

    StringJoiner colsAndTypes = new StringJoiner(",");

    ((Iterator<Column>) table.getColumnIterator()).forEachRemaining(col -> colsAndTypes
        .add(col.getQuotedName()
            + " " + col.getSqlType(this.spannerDialect, metadata)
            + (col.isNullable() ? this.spannerDialect.getNullColumnString() : " not null")));

    return new String[]{
        MessageFormat.format(createTableTemplate, table.getQuotedName(), colsAndTypes.toString(),
            primaryKeyColNames)};
  }

  @Override
  public String[] getSqlDropStrings(Table table, Metadata metadata) {
    /* Cloud Spanner requires examining the metadata to find all indexes and interleaved tables.
     * These must be dropped before the given table can be dropped.
     *
     * The current implementation does not support indexes or interleaved tables and indexes.
     * */

    return new String[]{this.spannerDialect.getDropTableString(table.getQuotedName())};
  }
}
