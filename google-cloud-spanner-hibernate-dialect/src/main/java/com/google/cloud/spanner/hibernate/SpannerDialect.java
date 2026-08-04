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

import com.google.cloud.spanner.hibernate.hints.ReplaceQueryPartsHint;
import com.google.cloud.spanner.hibernate.schema.SpannerForeignKeyExporter;
import com.google.common.base.Strings;
import java.sql.Connection;
import java.sql.SQLException;
import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.MultiTableHandlerBuildResult;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.spi.Exporter;
import org.jboss.logging.Logger;

/** Hibernate 7.x dialect for Cloud Spanner. */
public class SpannerDialect extends org.hibernate.dialect.SpannerDialect {
  private static final Logger LOG = Logger.getLogger(SpannerDialect.class.getName());

  private static class NoOpSqmMultiTableInsertStrategy implements SqmMultiTableInsertStrategy {
    private static final NoOpSqmMultiTableInsertStrategy INSTANCE =
        new NoOpSqmMultiTableInsertStrategy();

    @Override
    public MultiTableHandlerBuildResult buildHandler(
        SqmInsertStatement<?> sqmInsertStatement,
        DomainParameterXref domainParameterXref,
        DomainQueryExecutionContext domainQueryExecutionContext) {
      throw new HibernateException("Multi-table inserts are not supported for Cloud Spanner");
    }
  }

  /**
   * Property name that can be used to disable sequence support in the Cloud Spanner dialect. You
   * can use this temporarily if you have an existing database that already uses table-backed
   * emulated sequences without an explicit table generator. The long-term solution is to either
   * migrate to using actual sequences, or configuring your entities with an explicit {@link
   * org.hibernate.id.enhanced.TableGenerator}.
   */
  public static String SPANNER_DISABLE_SEQUENCES_PROPERTY = "hibernate.spanner.disable_sequences";

  /** Disables support for sequences for the {@link SpannerDialect}. */
  public static void disableSpannerSequences() {
    System.setProperty(SPANNER_DISABLE_SEQUENCES_PROPERTY, "true");
  }

  /**
   * Enables support for sequences for the {@link SpannerDialect}. Sequences are enabled by default,
   * and you only need to call this method if you have previously disabled them.
   */
  public static void enableSpannerSequences() {
    System.setProperty(SPANNER_DISABLE_SEQUENCES_PROPERTY, "false");
  }

  private final SpannerTableExporter spannerTableExporter = new SpannerTableExporter(this);

  private final SpannerForeignKeyExporter spannerForeignKeyExporter =
      new SpannerForeignKeyExporter(this);

  private final SpannerSequenceSupport sequenceSupport = new SpannerSequenceSupport();

  private final SpannerUniqueDelegate spannerUniqueDelegate = new SpannerUniqueDelegate(this);

  /** Default constructor. */
  public SpannerDialect() {}

  /** Constructor used for automatic dialect detection. */
  public SpannerDialect(DialectResolutionInfo info) {
    super(info);
  }

  @Override
  public Exporter<Table> getTableExporter() {
    return this.spannerTableExporter;
  }

  @Override
  public Exporter<ForeignKey> getForeignKeyExporter() {
    return this.spannerForeignKeyExporter;
  }

  @Override
  public SpannerSequenceSupport getSequenceSupport() {
    return this.sequenceSupport;
  }

  // Overridden to return standard "create unique index" instead of upstream's
  // "create unique null_filtered index", maintaining compatibility with existing
  // Cloud Spanner unique index schemas and DDL migrations.
  @Override
  public String getCreateIndexString(boolean unique) {
    return unique ? "create unique index" : "create index";
  }

  // Upstream Hibernate sets PREFERRED_POOLED_OPTIMIZER to "none", which causes default
  // @GeneratedValue(strategy = GenerationType.AUTO) annotations to default to an increment
  // size of 1 and use native Spanner sequences (CREATE SEQUENCE ...).
  //
  // Overriding to restore Hibernate's standard default of "pooled-lo" makes default
  // @GeneratedValue annotations use table-backed sequences (CREATE TABLE ..._SEQ) with
  // increment size 50, maintaining backwards compatibility with existing user schemas.
  @Override
  protected void initDefaultProperties() {
    super.initDefaultProperties();
    getDefaultProperties().setProperty(AvailableSettings.PREFERRED_POOLED_OPTIMIZER, "pooled-lo");
  }

  private static final class SpannerSequenceInformationExtractor
      extends SequenceInformationExtractorLegacyImpl {

    private static final SpannerSequenceInformationExtractor INSTANCE =
        new SpannerSequenceInformationExtractor();

    @Override
    public Iterable<SequenceInformation> extractMetadata(ExtractionContext extractionContext)
        throws SQLException {
      // Queries on INFORMATION_SCHEMA should use single-use read-only transactions.
      // In JDBC, the easiest way to achieve that is to use auto-commit.
      Connection connection = extractionContext.getJdbcConnection();
      boolean autoCommit = connection.getAutoCommit();
      try {
        connection.setAutoCommit(true);
        return super.extractMetadata(extractionContext);
      } finally {
        connection.setAutoCommit(autoCommit);
      }
    }
  }

  @Override
  public SequenceInformationExtractor getSequenceInformationExtractor() {
    return getQuerySequencesString() == null
        ? SequenceInformationExtractorNoOpImpl.INSTANCE
        : SpannerSequenceInformationExtractor.INSTANCE;
  }

  @Override
  public UniqueDelegate getUniqueDelegate() {
    return spannerUniqueDelegate;
  }

  @Override
  public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
      EntityMappingType entityDescriptor, RuntimeModelCreationContext runtimeModelCreationContext) {
    return NoOpSqmMultiTableInsertStrategy.INSTANCE;
  }

  @Override
  public String addSqlHintOrComment(
      String sql, QueryOptions queryOptions, boolean commentsEnabled) {
    if (hasStatementHint(queryOptions)) {
      sql = queryOptions.getComment() + sql;
    } else {
      if (hasCommentHint(queryOptions)) {
        sql = applyHint(sql, queryOptions.getComment());
      }
      if (queryOptions.getDatabaseHints() != null && !queryOptions.getDatabaseHints().isEmpty()) {
        sql = applyQueryHints(sql, queryOptions);
      }
    }
    return super.addSqlHintOrComment(sql, queryOptions, commentsEnabled);
  }

  private static String applyHint(String sql, String hint) {
    try {
      return ReplaceQueryPartsHint.fromComment(hint).replace(sql);
    } catch (Throwable hintParseError) {
      // Just log and continue with the query normally.
      // The reason that we ignore 'invalid' hints is that we don't know whether it actually is a
      // hint, or just happened to be a comment that looked at least a bit like a hint.
      LOG.warnf("Potential invalid hint found: %s", hint);
    }
    return sql;
  }

  private static String applyQueryHints(String sql, QueryOptions queryOptions) {
    for (String hint : queryOptions.getDatabaseHints()) {
      if (stringCouldContainReplacementHint(hint)) {
        sql = applyHint(sql, hint);
      }
    }
    return sql;
  }

  private static boolean hasCommentHint(QueryOptions queryOptions) {
    return stringCouldContainReplacementHint(queryOptions.getComment());
  }

  private static boolean stringCouldContainReplacementHint(String hint) {
    return !Strings.isNullOrEmpty(hint)
        && hint.contains("{")
        && hint.contains("}")
        && hint.contains(ReplaceQueryPartsHint.SPANNER_REPLACEMENTS_FIELD_NAME);
  }

  private static boolean hasStatementHint(QueryOptions queryOptions) {
    return hasStatementHint(queryOptions.getComment());
  }

  private static boolean hasStatementHint(String hint) {
    return !Strings.isNullOrEmpty(hint) && hint.startsWith("@{") && hint.endsWith("}");
  }

  @Override
  public boolean canCreateSchema() {
    return true;
  }

  @Override
  public String[] getCreateSchemaCommand(String schemaName) {
    return new String[] {"CREATE SCHEMA IF NOT EXISTS " + quote(schemaName)};
  }

  @Override
  public String[] getDropSchemaCommand(String schemaName) {
    return new String[] {"DROP SCHEMA IF EXISTS " + quote(schemaName)};
  }

  @Override
  public boolean qualifyIndexName() {
    return true;
  }
}
