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

import org.hibernate.boot.Metadata;
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
	public String[] getSqlCreateStrings(Table exportable, Metadata metadata) {
		return new String[]{"test_placeholder"};
	}

	@Override
	public String[] getSqlDropStrings(Table table, Metadata metadata) {
		/*
		 * Cloud Spanner requires examining the metadata to find all indexes and interleaved tables.
		 * These must be dropped before the given table can be dropped.
		 */

		return new String[]{"test_placeholder"};
	}
}
