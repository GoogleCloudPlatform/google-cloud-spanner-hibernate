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

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.tool.schema.internal.StandardSequenceExporter;

/**
 * Sequence exporter for Cloud Spanner. This exporter treats any information in the catalog name as
 * additional options for the sequence.
 */
public class SpannerSequenceExporter extends StandardSequenceExporter {

  private final SpannerDialect dialect;

  public SpannerSequenceExporter(SpannerDialect dialect) {
    super(dialect);
    this.dialect = dialect;
  }

  @Override
  public String[] getSqlCreateStrings(Sequence sequence, Metadata metadata,
      SqlStringGenerationContext context) {
    Identifier catalog = sequence.getName().getCatalogName();
    if (catalog != null && !"".equals(catalog.getText())) {
      // Catalogs are not supported in Cloud Spanner, so we (mis-)use this field to store additional
      // options for the sequence.
      sequence = new Sequence("", null, sequence.getName().getSchemaName(),
          sequence.getName().getObjectName(), sequence.getInitialValue(),
          sequence.getIncrementSize());
      return new String[]{dialect.getSequenceSupport().getCreateSequenceString(
          getFormattedSequenceName(sequence.getName(), metadata, context),
          sequence.getInitialValue(), catalog.getText())};
    }
    return super.getSqlCreateStrings(sequence, metadata, context);
  }

}
