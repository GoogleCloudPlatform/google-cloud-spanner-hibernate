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

import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.NUMERIC;

import com.google.cloud.spanner.hibernate.schema.SpannerForeignKeyExporter;
import com.google.cloud.spanner.jdbc.JsonType;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.hibernate.HibernateException;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
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

/** Hibernate 6.x dialect for Cloud Spanner. */
public class SpannerDialect extends org.hibernate.dialect.SpannerDialect {
  private static class NoOpSqmMultiTableInsertStrategy implements SqmMultiTableInsertStrategy {
    private static final NoOpSqmMultiTableInsertStrategy INSTANCE =
        new NoOpSqmMultiTableInsertStrategy();

    @Override
    public int executeInsert(
        SqmInsertStatement<?> sqmInsertStatement,
        DomainParameterXref domainParameterXref,
        DomainQueryExecutionContext context) {
      throw new HibernateException("Multi-table inserts are not supported for Cloud Spanner");
    }
  }

  private static class SpannerJsonJdbcType extends JsonAsStringJdbcType {
    private SpannerJsonJdbcType() {
      super(SqlTypes.LONG32VARCHAR, null);
    }

    @Override
    public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
      return new BasicBinder<X>(javaType, this) {
        @Override
        protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
            throws SQLException {
          final String json = ((SpannerJsonJdbcType) getJdbcType()).toString(
              value, getJavaType(), options);
          st.setObject(index, json, JsonType.VENDOR_TYPE_NUMBER);
        }

        @Override
        protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
            throws SQLException {
          final String json = ((SpannerJsonJdbcType) getJdbcType()).toString(
              value, getJavaType(), options);
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
   * migrate to using actual sequences, or configuring your entities with an explicit
   * {@link org.hibernate.id.enhanced.TableGenerator}.
   */
  public static String SPANNER_DISABLE_SEQUENCES_PROPERTY = "hibernate.spanner.disable_sequences";

  /**
   * Disables support for sequences for the {@link SpannerDialect}.
   */
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
  private final StandardSequenceExporter sequenceExporter = new SpannerSequenceExporter(this);

  private final SpannerUniqueDelegate spannerUniqueDelegate = new SpannerUniqueDelegate(this);

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
    return super.columnType(sqlTypeCode);
  }

  @Override
  protected void registerColumnTypes(
      TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
    super.registerColumnTypes(typeContributions, serviceRegistry);
    JdbcTypeRegistry jdbcTypeRegistry =
        typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
    jdbcTypeRegistry.addDescriptorIfAbsent(new SpannerJsonJdbcType());
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
        + "       ) as start_value, 1 as minimum_value, " + Long.MAX_VALUE + " as maximum_value,\n" 
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

  private static final class SpannerSequenceInformationExtractor extends
      SequenceInformationExtractorLegacyImpl {

    private static final SpannerSequenceInformationExtractor INSTANCE =
        new SpannerSequenceInformationExtractor();

    @Override
    public Iterable<SequenceInformation> extractMetadata(
        ExtractionContext extractionContext) throws SQLException {
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
  public Exporter<Constraint> getUniqueKeyExporter() {
    return spannerUniqueKeyExporter;
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
}
