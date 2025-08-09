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

import com.google.cloud.spanner.hibernate.schema.RunBatchDdl;
import com.google.cloud.spanner.hibernate.schema.SpannerDatabaseInfo;
import com.google.cloud.spanner.hibernate.schema.SpannerTableStatements;
import com.google.cloud.spanner.hibernate.schema.TableDependencyTracker;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * The exporter for Cloud Spanner CREATE and DROP table statements.
 *
 * @author Chengyuan Zhao
 */
public class SpannerTableExporter implements Exporter<Table> {

  private final SpannerTableStatements spannerTableStatements;

  private final TableDependencyTracker tableDependencyTracker;

  /**
   * Constructor.
   *
   * @param spannerDialect a Cloud Spanner dialect.
   */
  public SpannerTableExporter(SpannerDialect spannerDialect) {
    this.spannerTableStatements = new SpannerTableStatements(spannerDialect);
    this.tableDependencyTracker = new TableDependencyTracker();
  }

  @Override
  public String[] getSqlCreateStrings(
      Table currentTable, Metadata metadata, SqlStringGenerationContext context) {
    initializeUniqueConstraints(currentTable);
    List<String> sqlStrings = buildSqlStrings(currentTable, metadata, Action.CREATE);

    applyInitCommands(currentTable, metadata, context);

    return sqlStrings.toArray(new String[0]);
  }

  protected void applyInitCommands(
      Table table, Metadata metadata, SqlStringGenerationContext context) {
    List<InitCommand> initCommands = table.getInitCommands(context);
    for (InitCommand initCommand : initCommands) {
      addStatementAfterDdlBatch(metadata, initCommand.initCommands());
    }
  }

  @Override
  public String[] getSqlDropStrings(
      Table currentTable, Metadata metadata, SqlStringGenerationContext context) {
    initializeUniqueConstraints(currentTable);
    return buildSqlStrings(currentTable, metadata, Action.DROP).toArray(new String[0]);
  }

  /**
   * Initializes the table exporter for if a new create-table or drop-table sequence is starting.
   */
  public void init(
      Metadata metadata, SpannerDatabaseInfo spannerDatabaseInfo, Action schemaAction) {
    tableDependencyTracker.initializeDependencies(metadata, schemaAction);
    spannerTableStatements.initializeSpannerDatabaseInfo(spannerDatabaseInfo);
  }

  private List<String> buildSqlStrings(Table currentTable, Metadata metadata, Action schemaAction) {
    Collection<Table> tablesToProcess = tableDependencyTracker.getDependentTables(currentTable);

    return tablesToProcess.stream()
        .flatMap(
            table -> {
              if (schemaAction == Action.CREATE) {
                return spannerTableStatements.createTable(table, metadata).stream();
              } else {
                return spannerTableStatements.dropTable(table).stream();
              }
            })
        .collect(Collectors.toList());
  }

  static void addStatementAfterDdlBatch(Metadata metadata, String[] statements) {
    // Find the RunBatchDdl auxiliary object which can run statements after the DDL batch.
    Optional<RunBatchDdl> runBatchDdl =
        metadata.getDatabase().getAuxiliaryDatabaseObjects().stream()
            .filter(RunBatchDdl.class::isInstance)
            .map(obj -> (RunBatchDdl) obj)
            .findFirst();

    if (runBatchDdl.isPresent()) {
      for (String statement : statements) {
        runBatchDdl.get().addAfterDdlStatement(statement);
      }
    } else {
      throw new IllegalStateException(
          "Failed to generate statement to execute after DDL batch. "
              + "The Spanner dialect did not create auxiliary database objects correctly. "
              + "Please post a question to "
              + "https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/issues");
    }
  }

  /**
   * Processes the columns of the table and creates Unique Constraints for columns annotated
   * with @Column(unique = true).
   */
  private static void initializeUniqueConstraints(Table table) {
    for (Column column : table.getColumns()) {
      if (column.isUnique() && !table.isPrimaryKey(column)) {
        String uniqueKeyName = column.getUniqueKeyName();
        final String keyName =
            uniqueKeyName == null
                // fallback in case the ImplicitNamingStrategy name was not assigned
                // (we don't have access to the ImplicitNamingStrategy here)
                ? generateName("UK_", table, column)
                : uniqueKeyName;
        final UniqueKey uniqueKey = table.getOrCreateUniqueKey(keyName);
        uniqueKey.addColumn(column);
      }
    }
  }

  private static String generateName(String prefix, Table table, Column... columns) {
    // Use a concatenation that guarantees uniqueness, even if identical names
    // exist between all table and column identifiers.
    final StringBuilder sb = new StringBuilder("table`" + table.getName() + "`");
    // Ensure a consistent ordering of columns, regardless of the order
    // they were bound.
    // Clone the list, as sometimes a set of order-dependent Column
    // bindings are given.
    final Column[] alphabeticalColumns = columns.clone();
    Arrays.sort(alphabeticalColumns, Comparator.comparing(Column::getName));
    for (Column column : alphabeticalColumns) {
      final String columnName = column == null ? "" : column.getName();
      sb.append("column`").append(columnName).append("`");
    }
    return prefix + hashedName(sb.toString());
  }

  private static String hashedName(String name) {
    try {
      final MessageDigest md = MessageDigest.getInstance("MD5");
      md.reset();
      md.update(name.getBytes());
      final byte[] digest = md.digest();
      final BigInteger bigInt = new BigInteger(1, digest);
      // By converting to base 35 (full alphanumeric), we guarantee
      // that the length of the name will always be smaller than the 30
      // character identifier restriction enforced by a few dialects.
      return bigInt.toString(35);
    } catch (NoSuchAlgorithmException e) {
      throw new HibernateException("Unable to generate a hashed Constraint name", e);
    }
  }
}
