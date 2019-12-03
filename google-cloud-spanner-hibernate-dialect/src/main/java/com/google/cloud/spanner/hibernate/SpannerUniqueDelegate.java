package com.google.cloud.spanner.hibernate;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.unique.DefaultUniqueDelegate;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.UniqueKey;

public class SpannerUniqueDelegate extends DefaultUniqueDelegate {

  /**
   * Constructs the Spanner unique delegate responsible for generating statements for building
   * Unique indices.
   *
   * @param dialect The dialect for which we are handling unique constraints
   */
  public SpannerUniqueDelegate(Dialect dialect) {
    super(dialect);
  }

  @Override
  public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata) {
    return Index.buildSqlCreateIndexString(
        dialect, uniqueKey.getName(), uniqueKey.getTable(), uniqueKey.columnIterator(),
        uniqueKey.getColumnOrderMap(), true, metadata);
  }

  @Override
  public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata) {
    StringBuilder buf = new StringBuilder("DROP INDEX ");
    buf.append(dialect.quote(uniqueKey.getName()));
    return buf.toString();
  }
}
