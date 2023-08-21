/*
 * Copyright 2019-2020 Google LLC
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

import com.google.cloud.spanner.hibernate.schema.SpannerForeignKeyExporter;
import com.google.cloud.spanner.jdbc.JsonType;
import java.io.Serializable;
import java.sql.Types;
import java.util.Map;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.LockingStrategyException;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.tool.schema.internal.StandardSequenceExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.StandardBasicTypes;

/**
 * Hibernate Dialect implementation for Cloud Spanner.
 *
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 * @author Daniel Zou
 * @author Dmitry Solomakha
 */
public class SpannerDialect extends Dialect {

  private static final int STRING_MAX_LENGTH = 2621440;

  private static final int BYTES_MAX_LENGTH = 10485760;

  private final SpannerTableExporter spannerTableExporter =
      new SpannerTableExporter(this);

  private final SpannerForeignKeyExporter spannerForeignKeyExporter =
      new SpannerForeignKeyExporter(this);

  private final StandardSequenceExporter sequenceExporter = new StandardSequenceExporter(this);

  private static final LockingStrategy LOCKING_STRATEGY = new DoNothingLockingStrategy();

  private static final Exporter NOOP_EXPORTER = new EmptyExporter();

  private final UniqueDelegate uniqueDelegate;

  /**
   * Default constructor for SpannerDialect.
   */
  public SpannerDialect() {
    registerColumnType(Types.ARRAY, "ARRAY");
    registerColumnType(Types.BOOLEAN, "BOOL");
    registerColumnType(Types.BIT, "BOOL");
    registerColumnType(Types.BIGINT, "INT64");
    registerColumnType(Types.SMALLINT, "INT64");
    registerColumnType(Types.TINYINT, "INT64");
    registerColumnType(Types.INTEGER, "INT64");
    registerColumnType(Types.CHAR, "STRING(1)");
    registerColumnType(Types.VARCHAR, STRING_MAX_LENGTH, "STRING($l)");
    registerColumnType(Types.NVARCHAR, STRING_MAX_LENGTH, "STRING($l)");
    registerColumnType(Types.FLOAT, "FLOAT64");
    registerColumnType(Types.DOUBLE, "FLOAT64");
    registerColumnType(Types.DATE, "DATE");
    registerColumnType(Types.TIME, "TIMESTAMP");
    registerColumnType(Types.TIMESTAMP, "TIMESTAMP");
    registerColumnType(Types.VARBINARY, BYTES_MAX_LENGTH, "BYTES($l)");
    registerColumnType(Types.BINARY, BYTES_MAX_LENGTH, "BYTES($l)");
    registerColumnType(Types.LONGVARCHAR, STRING_MAX_LENGTH, "STRING($l)");
    registerColumnType(Types.LONGVARBINARY, BYTES_MAX_LENGTH, "BYTES($l)");
    registerColumnType(Types.CLOB, "STRING(MAX)");
    registerColumnType(Types.NCLOB, "STRING(MAX)");
    registerColumnType(Types.BLOB, "BYTES(MAX)");
    registerColumnType(JsonType.VENDOR_TYPE_NUMBER, "JSON");

    registerColumnType(Types.DECIMAL, "NUMERIC");
    registerColumnType(Types.NUMERIC, "NUMERIC");

    registerFunction("ANY_VALUE", new StandardSQLFunction("ANY_VALUE"));
    registerFunction("COUNTIF", new StandardSQLFunction("COUNTIF", StandardBasicTypes.LONG));

    registerFunction("CONCAT", new StandardSQLFunction("CONCAT"));
    registerFunction("STRING_AGG",
        new StandardSQLFunction("STRING_AGG", StandardBasicTypes.STRING));
    registerFunction("FARM_FINGERPRINT",
        new StandardSQLFunction("FARM_FINGERPRINT", StandardBasicTypes.LONG));
    registerFunction("SHA1", new StandardSQLFunction("SHA1", StandardBasicTypes.BINARY));
    registerFunction("SHA256", new StandardSQLFunction("SHA256", StandardBasicTypes.BINARY));
    registerFunction("SHA512", new StandardSQLFunction("SHA512", StandardBasicTypes.BINARY));
    registerFunction("BYTE_LENGTH",
        new StandardSQLFunction("BYTE_LENGTH", StandardBasicTypes.LONG));
    registerFunction("CHAR_LENGTH",
        new StandardSQLFunction("CHAR_LENGTH", StandardBasicTypes.LONG));
    registerFunction("CHARACTER_LENGTH",
        new StandardSQLFunction("CHARACTER_LENGTH", StandardBasicTypes.LONG));
    registerFunction("CODE_POINTS_TO_BYTES",
        new StandardSQLFunction("CODE_POINTS_TO_BYTES", StandardBasicTypes.BINARY));
    registerFunction("CODE_POINTS_TO_STRING",
        new StandardSQLFunction("CODE_POINTS_TO_STRING", StandardBasicTypes.STRING));
    registerFunction("ENDS_WITH", new StandardSQLFunction("ENDS_WITH", StandardBasicTypes.BOOLEAN));
    registerFunction("FORMAT", new StandardSQLFunction("FORMAT", StandardBasicTypes.STRING));
    registerFunction("FROM_BASE64",
        new StandardSQLFunction("FROM_BASE64", StandardBasicTypes.BINARY));
    registerFunction("FROM_HEX", new StandardSQLFunction("FROM_HEX", StandardBasicTypes.BINARY));
    registerFunction("LENGTH", new StandardSQLFunction("LENGTH", StandardBasicTypes.LONG));
    registerFunction("LPAD", new StandardSQLFunction("LPAD"));
    registerFunction("LOCATE", new StandardSQLFunction("STRPOS", StandardBasicTypes.LONG));
    registerFunction("LOWER", new StandardSQLFunction("LOWER"));
    registerFunction("LTRIM", new StandardSQLFunction("LTRIM"));
    registerFunction("REGEXP_CONTAINS",
        new StandardSQLFunction("REGEXP_CONTAINS", StandardBasicTypes.BOOLEAN));
    registerFunction("REGEXP_EXTRACT", new StandardSQLFunction("REGEXP_EXTRACT"));
    registerFunction("REGEXP_REPLACE", new StandardSQLFunction("REGEXP_REPLACE"));
    registerFunction("REPLACE", new StandardSQLFunction("REPLACE"));
    registerFunction("REPEAT", new StandardSQLFunction("REPEAT"));
    registerFunction("REVERSE", new StandardSQLFunction("REVERSE"));
    registerFunction("RPAD", new StandardSQLFunction("RPAD"));
    registerFunction("RTRIM", new StandardSQLFunction("RTRIM"));
    registerFunction("SAFE_CONVERT_BYTES_TO_STRING",
        new StandardSQLFunction("SAFE_CONVERT_BYTES_TO_STRING", StandardBasicTypes.STRING));
    registerFunction("STARTS_WITH",
        new StandardSQLFunction("STARTS_WITH", StandardBasicTypes.BOOLEAN));
    registerFunction("STR",
        new SQLFunctionTemplate(StandardBasicTypes.STRING, "cast(?1 as string)"));
    registerFunction("STRPOS", new StandardSQLFunction("STRPOS", StandardBasicTypes.LONG));
    registerFunction("SUBSTR", new StandardSQLFunction("SUBSTR", StandardBasicTypes.STRING));
    registerFunction("SUBSTRING", new StandardSQLFunction("SUBSTR", StandardBasicTypes.STRING));
    registerFunction("TO_BASE64", new StandardSQLFunction("TO_BASE64", StandardBasicTypes.STRING));

    registerFunction("TO_HEX", new StandardSQLFunction("TO_HEX", StandardBasicTypes.STRING));
    registerFunction("TRIM", new StandardSQLFunction("TRIM"));
    registerFunction("UPPER", new StandardSQLFunction("UPPER"));
    registerFunction("JSON_QUERY",
        new StandardSQLFunction("JSON_QUERY", StandardBasicTypes.STRING));
    registerFunction("JSON_VALUE",
        new StandardSQLFunction("JSON_VALUE", StandardBasicTypes.STRING));
    registerFunction("ARRAY_CONCAT", new StandardSQLFunction("ARRAY_CONCAT"));
    registerFunction("ARRAY_LENGTH",
        new StandardSQLFunction("ARRAY_LENGTH", StandardBasicTypes.LONG));
    registerFunction("ARRAY_TO_STRING",
        new StandardSQLFunction("ARRAY_TO_STRING", StandardBasicTypes.STRING));

    registerFunction("ARRAY_REVERSE", new StandardSQLFunction("ARRAY_REVERSE"));

    registerFunction("CURRENT_DATE",
        new StandardSQLFunction("CURRENT_DATE", StandardBasicTypes.DATE));
    registerFunction("EXTRACT",
        new SQLFunctionTemplate(StandardBasicTypes.LONG, "extract(?1 ?2 ?3)"));
    registerFunction("DATE", new StandardSQLFunction("DATE", StandardBasicTypes.DATE));
    registerFunction("DATE_ADD", new StandardSQLFunction("DATE_ADD", StandardBasicTypes.DATE));
    registerFunction("DATE_SUB", new StandardSQLFunction("DATE_SUB", StandardBasicTypes.DATE));
    registerFunction("DATE_DIFF", new StandardSQLFunction("DATE_DIFF", StandardBasicTypes.LONG));
    registerFunction("DATE_TRUNC", new StandardSQLFunction("DATE_TRUNC", StandardBasicTypes.DATE));
    registerFunction("DATE_FROM_UNIX_DATE",
        new StandardSQLFunction("DATE_FROM_UNIX_DATE", StandardBasicTypes.DATE));
    registerFunction("FORMAT_DATE",
        new StandardSQLFunction("FORMAT_DATE", StandardBasicTypes.STRING));
    registerFunction("PARSE_DATE", new StandardSQLFunction("PARSE_DATE", StandardBasicTypes.DATE));
    registerFunction("UNIX_DATE", new StandardSQLFunction("UNIX_DATE", StandardBasicTypes.LONG));

    registerFunction("CURRENT_TIME",
        new StandardSQLFunction("CURRENT_TIMESTAMP", StandardBasicTypes.TIMESTAMP));
    registerFunction("CURRENT_TIMESTAMP",
        new StandardSQLFunction("CURRENT_TIMESTAMP", StandardBasicTypes.TIMESTAMP));
    registerFunction("STRING", new StandardSQLFunction("STRING", StandardBasicTypes.STRING));
    registerFunction("TIMESTAMP",
        new StandardSQLFunction("TIMESTAMP", StandardBasicTypes.TIMESTAMP));
    registerFunction("TIMESTAMP_ADD",
        new StandardSQLFunction("TIMESTAMP_ADD", StandardBasicTypes.TIMESTAMP));
    registerFunction("TIMESTAMP_SUB",
        new StandardSQLFunction("TIMESTAMP_SUB", StandardBasicTypes.TIMESTAMP));
    registerFunction("TIMESTAMP_DIFF",
        new StandardSQLFunction("TIMESTAMP_DIFF", StandardBasicTypes.LONG));
    registerFunction("TIMESTAMP_TRUNC",
        new StandardSQLFunction("TIMESTAMP_TRUNC", StandardBasicTypes.TIMESTAMP));
    registerFunction("FORMAT_TIMESTAMP",
        new StandardSQLFunction("FORMAT_TIMESTAMP", StandardBasicTypes.STRING));
    registerFunction("PARSE_TIMESTAMP",
        new StandardSQLFunction("PARSE_TIMESTAMP", StandardBasicTypes.TIMESTAMP));
    registerFunction("TIMESTAMP_SECONDS",
        new StandardSQLFunction("TIMESTAMP_SECONDS", StandardBasicTypes.TIMESTAMP));
    registerFunction("TIMESTAMP_MILLIS",
        new StandardSQLFunction("TIMESTAMP_MILLIS", StandardBasicTypes.TIMESTAMP));
    registerFunction("TIMESTAMP_MICROS",
        new StandardSQLFunction("TIMESTAMP_MICROS", StandardBasicTypes.TIMESTAMP));
    registerFunction("UNIX_SECONDS",
        new StandardSQLFunction("UNIX_SECONDS", StandardBasicTypes.LONG));
    registerFunction("UNIX_MILLIS",
        new StandardSQLFunction("UNIX_MILLIS", StandardBasicTypes.LONG));
    registerFunction("UNIX_MICROS",
        new StandardSQLFunction("UNIX_MICROS", StandardBasicTypes.LONG));
    registerFunction("PARSE_TIMESTAMP",
        new StandardSQLFunction("PARSE_TIMESTAMP", StandardBasicTypes.TIMESTAMP));

    registerFunction("BIT_AND", new StandardSQLFunction("BIT_AND", StandardBasicTypes.LONG));
    registerFunction("BIT_OR", new StandardSQLFunction("BIT_OR", StandardBasicTypes.LONG));
    registerFunction("BIT_XOR", new StandardSQLFunction("BIT_XOR", StandardBasicTypes.LONG));
    registerFunction("LOGICAL_AND",
        new StandardSQLFunction("LOGICAL_AND", StandardBasicTypes.BOOLEAN));
    registerFunction("LOGICAL_OR",
        new StandardSQLFunction("LOGICAL_OR", StandardBasicTypes.BOOLEAN));

    registerFunction("IS_INF", new StandardSQLFunction("IS_INF", StandardBasicTypes.BOOLEAN));
    registerFunction("IS_NAN", new StandardSQLFunction("IS_NAN", StandardBasicTypes.BOOLEAN));

    registerFunction("SIGN", new StandardSQLFunction("SIGN"));
    registerFunction("IEEE_DIVIDE",
        new StandardSQLFunction("IEEE_DIVIDE", StandardBasicTypes.DOUBLE));
    registerFunction("SQRT", new StandardSQLFunction("SQRT", StandardBasicTypes.DOUBLE));
    registerFunction("POW", new StandardSQLFunction("POW", StandardBasicTypes.DOUBLE));
    registerFunction("POWER", new StandardSQLFunction("POWER", StandardBasicTypes.DOUBLE));
    registerFunction("EXP", new StandardSQLFunction("EXP", StandardBasicTypes.DOUBLE));
    registerFunction("LN", new StandardSQLFunction("LN", StandardBasicTypes.DOUBLE));
    registerFunction("LOG", new StandardSQLFunction("LOG", StandardBasicTypes.DOUBLE));
    registerFunction("LOG10", new StandardSQLFunction("LOG10", StandardBasicTypes.DOUBLE));
    registerFunction("GREATEST", new StandardSQLFunction("GREATEST"));
    registerFunction("LEAST", new StandardSQLFunction("LEAST"));
    registerFunction("DIV", new StandardSQLFunction("DIV", StandardBasicTypes.LONG));
    registerFunction("MOD", new StandardSQLFunction("MOD", StandardBasicTypes.LONG));
    registerFunction("ROUND", new StandardSQLFunction("ROUND", StandardBasicTypes.DOUBLE));
    registerFunction("TRUNC", new StandardSQLFunction("TRUNC", StandardBasicTypes.DOUBLE));
    registerFunction("CEIL", new StandardSQLFunction("CEIL", StandardBasicTypes.DOUBLE));
    registerFunction("CEILING", new StandardSQLFunction("CEILING", StandardBasicTypes.DOUBLE));
    registerFunction("FLOOR", new StandardSQLFunction("FLOOR", StandardBasicTypes.DOUBLE));
    registerFunction("COS", new StandardSQLFunction("COS", StandardBasicTypes.DOUBLE));
    registerFunction("COSH", new StandardSQLFunction("COSH", StandardBasicTypes.DOUBLE));
    registerFunction("ACOS", new StandardSQLFunction("ACOS", StandardBasicTypes.DOUBLE));
    registerFunction("ACOSH", new StandardSQLFunction("ACOSH", StandardBasicTypes.DOUBLE));
    registerFunction("SIN", new StandardSQLFunction("SIN", StandardBasicTypes.DOUBLE));
    registerFunction("SINH", new StandardSQLFunction("SINH", StandardBasicTypes.DOUBLE));
    registerFunction("ASIN", new StandardSQLFunction("ASIN", StandardBasicTypes.DOUBLE));
    registerFunction("ASINH", new StandardSQLFunction("ASINH", StandardBasicTypes.DOUBLE));
    registerFunction("TAN", new StandardSQLFunction("TAN", StandardBasicTypes.DOUBLE));
    registerFunction("TANH", new StandardSQLFunction("TANH", StandardBasicTypes.DOUBLE));
    registerFunction("ATAN", new StandardSQLFunction("ATAN", StandardBasicTypes.DOUBLE));
    registerFunction("ATANH", new StandardSQLFunction("ATANH", StandardBasicTypes.DOUBLE));
    registerFunction("ATAN2", new StandardSQLFunction("ATAN2", StandardBasicTypes.DOUBLE));

    this.uniqueDelegate = new SpannerUniqueDelegate(this);
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
  protected String getCreateSequenceString(String sequenceName) throws MappingException {
    return "create sequence " + sequenceName + " options(sequence_kind=\"bit_reversed_positive\")";
  }

  @Override
  protected String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize)
      throws MappingException {
    if (initialValue == 1 && incrementSize == 1) {
      return getCreateSequenceString(sequenceName);
    }
    return super.getCreateSequenceString(sequenceName, initialValue, incrementSize);
  }

  @Override
  public String getDropSequenceString(String sequenceName) {
    return "drop sequence " + sequenceName;
  }

  @Override
  public String getSequenceNextValString(String sequenceName) {
    return "select " + getSelectSequenceNextValString( sequenceName );
  }

  @Override
  public String getSelectSequenceNextValString(String sequenceName) {
    return "get_next_sequence_value(sequence " + sequenceName + ")";
  }

  @Override
  public boolean supportsSequences() {
    String disableSequences = System.getProperty("hibernate.spanner.disable_sequences", "false");
    try {
      return !Boolean.parseBoolean(disableSequences);
    } catch (Throwable ignore) {
      return true;
    }
  }

  @Override
  public boolean supportsPooledSequences() {
    // 'Pooled' sequences support an increment size > 1.
    return false;
  }

  @Override
  public String getQuerySequencesString() {
    return "select catalog as sequence_catalog, schema as sequence_schema, name as sequence_name, "
        + "1 as start_value, 1 as minimum_value, " + Long.MAX_VALUE + " as maximum_value, "
        + "1 as increment "
        + "from information_schema.sequences";
  }

  /* SELECT-related functions */

  @Override
  public boolean supportsCurrentTimestampSelection() {
    return true;
  }

  @Override
  public boolean isCurrentTimestampSelectStringCallable() {
    return false;
  }

  @Override
  public String getCurrentTimestampSelectString() {
    return "SELECT CURRENT_TIMESTAMP() as now";
  }

  @Override
  public String toBooleanValueString(boolean bool) {
    return bool ? "TRUE" : "FALSE";
  }

  @Override
  public boolean supportsUnionAll() {
    return true;
  }

  @Override
  public boolean supportsCaseInsensitiveLike() {
    return false;
  }

  /* DDL-related functions */

  @Override
  public boolean canCreateSchema() {
    return false;
  }

  @Override
  public String[] getCreateSchemaCommand(String schemaName) {
    throw new UnsupportedOperationException(
        "No create schema syntax supported by " + getClass().getName());
  }

  @Override
  public String[] getDropSchemaCommand(String schemaName) {
    throw new UnsupportedOperationException(
        "No drop schema syntax supported by " + getClass().getName());
  }

  @Override
  public String getCurrentSchemaCommand() {
    throw new UnsupportedOperationException("No current schema syntax supported by "
        + getClass().getName());
  }

  @Override
  public SchemaNameResolver getSchemaNameResolver() {
    // Spanner does not have a notion of database name schemas, so return "".
    return (connection, dialect) -> "";
  }

  @Override
  public boolean qualifyIndexName() {
    return false;
  }

  @Override
  public String getAddColumnString() {
    return "ADD COLUMN";
  }

  @Override
  public String getAddForeignKeyConstraintString(String constraintName,
      String[] foreignKey,
      String referencedTable,
      String[] primaryKey,
      boolean referencesPrimaryKey) {
    return super.getAddForeignKeyConstraintString(
        constraintName, foreignKey, referencedTable, primaryKey, false);
  }

  @Override
  public String getAddPrimaryKeyConstraintString(String constraintName) {
    throw new UnsupportedOperationException("Cannot add primary key constraint in Cloud Spanner.");
  }

  /* Lock acquisition functions */

  @Override
  public boolean supportsLockTimeouts() {
    return false;
  }

  @Override
  public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
    return LOCKING_STRATEGY;
  }

  @Override
  public String getForUpdateString(LockOptions lockOptions) {
    return "";
  }

  @Override
  public String getForUpdateString() {
    return "";
  }

  @Override
  public String getForUpdateString(String aliases) {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }

  @Override
  public String getForUpdateString(String aliases, LockOptions lockOptions) {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }

  @Override
  public String getWriteLockString(int timeout) {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }

  @Override
  public String getWriteLockString(String aliases, int timeout) {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }

  @Override
  public String getReadLockString(int timeout) {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }

  @Override
  public String getReadLockString(String aliases, int timeout) {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }

  @Override
  public boolean supportsOuterJoinForUpdate() {
    return false;
  }

  @Override
  public String getForUpdateNowaitString() {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }

  @Override
  public String getForUpdateNowaitString(String aliases) {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }


  @Override
  public String getForUpdateSkipLockedString() {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }

  @Override
  public String getForUpdateSkipLockedString(String aliases) {
    throw new UnsupportedOperationException("Cloud Spanner does not support selecting for lock"
        + " acquisition.");
  }

  /* Unsupported Hibernate Exporters */

  @Override
  public String applyLocksToSql(String sql, LockOptions aliasedLockOptions,
      Map<String, String[]> keyColumnNames) {
    return sql;
  }

  @Override
  public UniqueDelegate getUniqueDelegate() {
    return uniqueDelegate;
  }

  @Override
  public boolean supportsCircularCascadeDeleteConstraints() {
    return false;
  }

  @Override
  public boolean supportsCascadeDelete() {
    return true;
  }

  @Override
  public char openQuote() {
    return '`';
  }

  @Override
  public char closeQuote() {
    return '`';
  }

  /* Limits and offsets */

  @Override
  public boolean supportsLimit() {
    return true;
  }

  @Override
  public boolean supportsLimitOffset() {
    return true;
  }

  @Override
  public boolean supportsVariableLimit() {
    return true;
  }

  @Override
  public String getLimitString(String sql, boolean hasOffset) {
    return sql + (hasOffset ? " limit ? offset ?" : " limit ?");
  }

  @Override
  // Returns true because the correct order is [limit, offset]
  // https://cloud.google.com/spanner/docs/query-syntax#limit_and_offset_clause
  public boolean bindLimitParametersInReverseOrder() {
    return true;
  }

  /* Type conversion and casting */

  @Override
  public String getCastTypeName(int code) {
    switch (code) {
      case Types.VARCHAR:
        return "STRING";
      default:
        return super.getCastTypeName(code);
    }
  }

  /**
   * A locking strategy for the Cloud Spanner dialect that does nothing. Cloud Spanner does not
   * support locking.
   *
   * @author Chengyuan Zhao
   */
  static class DoNothingLockingStrategy implements LockingStrategy {

    @Override
    public void lock(Serializable id, Object version, Object object, int timeout,
        SharedSessionContractImplementor session)
        throws StaleObjectStateException, LockingStrategyException {
      // Do nothing. Cloud Spanner doesn't have have locking strategies.
    }
  }

  /**
   * A no-op {@link Exporter} which is responsible for returning empty Create and Drop SQL strings.
   *
   * @author Daniel Zou
   */
  static class EmptyExporter<T extends Exportable> implements Exporter<T> {

    @Override
    public String[] getSqlCreateStrings(T exportable, Metadata metadata) {
      return new String[0];
    }

    @Override
    public String[] getSqlDropStrings(T exportable, Metadata metadata) {
      return new String[0];
    }
  }
}
