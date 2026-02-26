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

import static java.sql.Types.REAL;
import static org.hibernate.sql.ast.internal.NonLockingClauseStrategy.NON_CLAUSE_STRATEGY;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.NUMERIC;

import com.google.cloud.spanner.hibernate.hints.ReplaceQueryPartsHint;
import com.google.cloud.spanner.hibernate.schema.SpannerForeignKeyExporter;
import com.google.cloud.spanner.jdbc.JsonType;
import com.google.common.base.Strings;
import jakarta.persistence.Timeout;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.Timeouts;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.internal.LockingSupportSimple;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.MultiTableHandlerBuildResult;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.internal.PessimisticLockKind;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.internal.StandardSequenceExporter;
import org.hibernate.tool.schema.internal.StandardUniqueKeyExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.JsonAsStringJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
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

  private static class SpannerJsonJdbcType extends JsonAsStringJdbcType {
    private SpannerJsonJdbcType() {
      super(JSON, null);
    }

    @Override
    public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
      return new BasicBinder<X>(javaType, this) {
        @Override
        protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
            throws SQLException {
          final String json =
              ((SpannerJsonJdbcType) getJdbcType()).toString(value, getJavaType(), options);
          st.setObject(index, json, JsonType.VENDOR_TYPE_NUMBER);
        }

        @Override
        protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
            throws SQLException {
          final String json =
              ((SpannerJsonJdbcType) getJdbcType()).toString(value, getJavaType(), options);
          st.setObject(name, json, JsonType.VENDOR_TYPE_NUMBER);
        }

        @Override
        protected void doBindNull(PreparedStatement st, int index, WrapperOptions options)
            throws SQLException {
          st.setNull(index, JsonType.VENDOR_TYPE_NUMBER);
        }

        @Override
        protected void doBindNull(CallableStatement st, String name, WrapperOptions options)
            throws SQLException {
          st.setNull(name, JsonType.VENDOR_TYPE_NUMBER);
        }
      };
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

  private final StandardUniqueKeyExporter spannerUniqueKeyExporter =
      new StandardUniqueKeyExporter(this);

  private final SpannerSequenceSupport sequenceSupport = new SpannerSequenceSupport();

  private final StandardSequenceExporter sequenceExporter = new StandardSequenceExporter(this);

  private final SpannerUniqueDelegate spannerUniqueDelegate = new SpannerUniqueDelegate(this);

  private final LockingSupport spannerLockingSupport =
      new LockingSupportSimple(
          PessimisticLockStyle.CLAUSE,
          RowLockStrategy.NONE,
          LockTimeoutType.NONE,
          OuterJoinLockingType.FULL,
          ConnectionLockTimeoutStrategy.NONE);

  /** Default constructor. */
  public SpannerDialect() {}

  /** Constructor used for automatic dialect detection. */
  public SpannerDialect(DialectResolutionInfo info) {
    super(info);
  }

  @Override
  public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
    return new StandardSqlAstTranslatorFactory() {
      @Override
      protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
          SessionFactoryImplementor sessionFactory, Statement statement) {
        return new SpannerSqlAstTranslator<>(sessionFactory, statement);
      }
    };
  }

  // TODO: Remove when the override in the super class has been fixed.
  @Override
  protected String columnType(int sqlTypeCode) {
    if (sqlTypeCode == DECIMAL || sqlTypeCode == NUMERIC) {
      return "numeric";
    }
    if (sqlTypeCode == JSON) {
      return "json";
    }
    // The JDBC spec is a bit confusing here.
    // DOUBLE == FLOAT == 64 bit
    // REAL == 32 bit
    // The default Hibernate implementation did not really get this right, as it uses
    // java.sql.Types.FLOAT for java.lang.Float. It should have been java.sql.Types.REAL.
    // This dialect follows the default Hibernate implementation, and in order to actually
    // use a float32, you need to annotate the column with the JDBC type code REAL.
    if (sqlTypeCode == REAL) {
      return "float32";
    }
    return super.columnType(sqlTypeCode);
  }

  @Override
  protected void registerColumnTypes(
      TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
    super.registerColumnTypes(typeContributions, serviceRegistry);
    JdbcTypeRegistry jdbcTypeRegistry =
        typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
    jdbcTypeRegistry.addDescriptorIfAbsent(new SpannerJsonJdbcType());
    final DdlTypeRegistry ddlTypeRegistry =
        typeContributions.getTypeConfiguration().getDdlTypeRegistry();
    ddlTypeRegistry.addDescriptor(
        new DdlTypeImpl(SqlTypes.JSON, columnType(SqlTypes.JSON), castType(SqlTypes.JSON), this));
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
  public Exporter<Sequence> getSequenceExporter() {
    return this.sequenceExporter;
  }

  @Override
  public SpannerSequenceSupport getSequenceSupport() {
    return this.sequenceSupport;
  }

  @Override
  public String getQuerySequencesString() {
    return "select seq.CATALOG as sequence_catalog, "
        + "seq.SCHEMA as sequence_schema, "
        + "seq.NAME as sequence_name,\n"
        + "       coalesce(kind.OPTION_VALUE, 'bit_reversed_positive') as KIND,\n"
        + "       coalesce(safe_cast(initial.OPTION_VALUE AS INT64),\n"
        + "           case coalesce(kind.OPTION_VALUE, 'bit_reversed_positive')\n"
        + "               when 'bit_reversed_positive' then 1\n"
        + "               when 'bit_reversed_signed' then -pow(2, 63)\n"
        + "               else 1\n"
        + "           end\n"
        + "       ) as start_value, 1 as minimum_value, "
        + Long.MAX_VALUE
        + " as maximum_value,\n"
        + "       1 as increment,\n"
        + "       safe_cast(skip_range_min.OPTION_VALUE as int64) as skip_range_min,\n"
        + "       safe_cast(skip_range_max.OPTION_VALUE as int64) as skip_range_max,\n"
        + "from INFORMATION_SCHEMA.SEQUENCES seq\n"
        + "left outer join INFORMATION_SCHEMA.SEQUENCE_OPTIONS kind\n"
        + "    on seq.CATALOG=kind.CATALOG and seq.SCHEMA=kind.SCHEMA and "
        + "seq.NAME=kind.NAME and kind.OPTION_NAME='sequence_kind'\n"
        + "left outer join INFORMATION_SCHEMA.SEQUENCE_OPTIONS initial\n"
        + "    on seq.CATALOG=initial.CATALOG and seq.SCHEMA=initial.SCHEMA "
        + "and seq.NAME=initial.NAME and initial.OPTION_NAME='start_with_counter'\n"
        + "left outer join INFORMATION_SCHEMA.SEQUENCE_OPTIONS skip_range_min\n"
        + "    on seq.CATALOG=skip_range_min.CATALOG and seq.SCHEMA=skip_range_min.SCHEMA "
        + "and seq.NAME=skip_range_min.NAME and skip_range_min.OPTION_NAME='skip_range_min'\n"
        + "left outer join INFORMATION_SCHEMA.SEQUENCE_OPTIONS skip_range_max\n"
        + "    on seq.CATALOG=skip_range_max.CATALOG and seq.SCHEMA=skip_range_max.SCHEMA "
        + "and seq.NAME=skip_range_max.NAME and skip_range_max.OPTION_NAME='skip_range_max'";
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
  public IdentityColumnSupport getIdentityColumnSupport() {
    return SpannerIdentityColumnSupport.INSTANCE;
  }

  @Override
  public Exporter<UniqueKey> getUniqueKeyExporter() {
    return spannerUniqueKeyExporter;
  }

  @Override
  public boolean dropConstraints() {
    return true;
  }

  @Override
  public String getDropForeignKeyString() {
    // TODO: Remove when the override in the super class has been fixed.
    return "drop constraint";
  }

  @Override
  public String getAddForeignKeyConstraintString(
      String constraintName,
      String[] foreignKey,
      String referencedTable,
      String[] primaryKey,
      boolean referencesPrimaryKey) {
    // TODO: Remove when the override in the super class has been fixed.
    return " add constraint "
        + quote(constraintName)
        + " foreign key ("
        + String.join(", ", foreignKey)
        + ") references "
        + referencedTable
        // Cloud Spanner requires the referenced columns to specified in all cases, including
        // if the foreign key is referencing the primary key of the referenced table.
        + " ("
        + String.join(", ", primaryKey)
        + ')';
  }

  @Override
  public String getAddForeignKeyConstraintString(
      String constraintName, String foreignKeyDefinition) {
    // TODO: Remove when the override in the super class has been fixed.
    return " add constraint " + quote(constraintName) + " " + foreignKeyDefinition;
  }

  @Override
  public boolean supportsCascadeDelete() {
    return true;
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
  public IdentifierHelper buildIdentifierHelper(
      IdentifierHelperBuilder builder, DatabaseMetaData metadata) throws SQLException {
    builder.applyReservedWords(metadata);
    builder.setAutoQuoteKeywords(true);
    builder.setAutoQuoteDollar(true);
    return super.buildIdentifierHelper(builder, metadata);
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

  /* Lock acquisition functions */

  private static void validateSpannerLockTimeout(int millis) {
    if (Timeouts.isRealTimeout(millis)) {
      throw new UnsupportedOperationException("Spanner does not support lock timeout.");
    }
    if (millis == Timeouts.SKIP_LOCKED_MILLI) {
      throw new UnsupportedOperationException("Spanner does not support skip locked.");
    }
    if (millis == Timeouts.NO_WAIT_MILLI) {
      throw new UnsupportedOperationException("Spanner does not support no wait.");
    }
  }

  @Override
  public LockingStrategy getLockingStrategy(
      EntityPersister lockable, LockMode lockMode, Locking.Scope lockScope) {
    return switch (lockMode) {
      case PESSIMISTIC_FORCE_INCREMENT ->
          buildPessimisticForceIncrementStrategy(lockable, lockMode, lockScope);
      case UPGRADE_NOWAIT, UPGRADE_SKIPLOCKED, PESSIMISTIC_WRITE ->
          buildPessimisticWriteStrategy(lockable, lockMode, lockScope);
      case PESSIMISTIC_READ -> buildPessimisticReadStrategy(lockable, lockMode, lockScope);
      case OPTIMISTIC_FORCE_INCREMENT -> buildOptimisticForceIncrementStrategy(lockable, lockMode);
      case OPTIMISTIC -> buildOptimisticStrategy(lockable, lockMode);
      case READ -> buildReadStrategy(lockable, lockMode, lockScope);
      default -> throw new IllegalArgumentException("Unsupported lock mode : " + lockMode);
    };
  }

  @Override
  public LockingStrategy getLockingStrategy(EntityPersister lockable, LockMode lockMode) {
    return getLockingStrategy(lockable, lockMode, Locking.Scope.ROOT_ONLY);
  }

  @Override
  public LockingSupport getLockingSupport() {
    return spannerLockingSupport;
  }

  @Override
  public LockingClauseStrategy getLockingClauseStrategy(
      QuerySpec querySpec, LockOptions lockOptions) {
    if (getPessimisticLockStyle() != PessimisticLockStyle.CLAUSE || lockOptions == null) {
      return NON_CLAUSE_STRATEGY;
    }

    final LockMode lockMode = lockOptions.getLockMode();
    final PessimisticLockKind lockKind = PessimisticLockKind.interpret(lockMode);
    if (lockKind == PessimisticLockKind.NONE) {
      return NON_CLAUSE_STRATEGY;
    }

    if (lockOptions.getTimeout() != null) {
      validateSpannerLockTimeout(lockOptions.getTimeout().milliseconds());
    }

    final RowLockStrategy rowLockStrategy = RowLockStrategy.NONE;
    return buildLockingClauseStrategy(
        lockKind, rowLockStrategy, lockOptions, querySpec.getRootPathsForLocking());
  }

  @Override
  public String getForUpdateString() {
    return " for update";
  }

  @Override
  public String getForUpdateString(LockOptions lockOptions) {
    if (lockOptions != null && lockOptions.getTimeout() != null) {
      final int millis = lockOptions.getTimeout().milliseconds();
      validateSpannerLockTimeout(millis);
    }
    return getForUpdateString();
  }

  @Override
  public String getForUpdateString(String aliases) {
    return getForUpdateString();
  }

  @Override
  public String getForUpdateString(String aliases, LockOptions lockOptions) {
    return getForUpdateString(lockOptions);
  }

  @Override
  public String getWriteLockString(int timeout) {
    validateSpannerLockTimeout(timeout);
    return getForUpdateString();
  }

  @Override
  public String getWriteLockString(Timeout timeout) {
    return getWriteLockString(timeout.milliseconds());
  }

  @Override
  public String getWriteLockString(String aliases, int timeout) {
    return getWriteLockString(timeout);
  }

  @Override
  public String getWriteLockString(String aliases, Timeout timeout) {
    return getWriteLockString(aliases, timeout.milliseconds());
  }

  @Override
  public String getReadLockString(int timeout) {
    validateSpannerLockTimeout(timeout);
    return getForUpdateString();
  }

  @Override
  public String getReadLockString(Timeout timeout) {
    return getReadLockString(timeout.milliseconds());
  }

  @Override
  public String getReadLockString(String aliases, int timeout) {
    return getReadLockString(timeout);
  }

  @Override
  public String getReadLockString(String aliases, Timeout timeout) {
    return getReadLockString(aliases, timeout.milliseconds());
  }

  @Override
  public String getForUpdateNowaitString() {
    throw new UnsupportedOperationException("Spanner does not support no wait.");
  }

  @Override
  public String getForUpdateNowaitString(String aliases) {
    throw new UnsupportedOperationException("Spanner does not support no wait.");
  }

  @Override
  public String getForUpdateSkipLockedString() {
    throw new UnsupportedOperationException("Spanner does not support skip locked.");
  }

  @Override
  public String getForUpdateSkipLockedString(String aliases) {
    throw new UnsupportedOperationException("Spanner does not support skip locked.");
  }
}
