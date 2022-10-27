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

import com.google.cloud.spanner.hibernate.BitReversedSequenceStyleGenerator.ReplaceInitCommand;
import com.google.cloud.spanner.hibernate.schema.RunBatchDdl;
import com.google.cloud.spanner.hibernate.schema.SpannerDatabaseInfo;
import com.google.cloud.spanner.hibernate.schema.SpannerTableStatements;
import com.google.cloud.spanner.hibernate.schema.TableDependencyTracker;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;
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
    // Use only the replaced commands if the list contains both normal InitCommands and
    // ReplaceInitCommands.
    if (initCommands.stream().anyMatch(ReplaceInitCommand.class::isInstance)
        && initCommands.stream().anyMatch(cmd -> !ReplaceInitCommand.class.isInstance(cmd))) {
      initCommands =
          initCommands.stream()
              .filter(ReplaceInitCommand.class::isInstance)
              .collect(Collectors.toList());
    } else if (initCommands.stream().anyMatch(ReplaceInitCommand.class::isInstance)) {
      // Only ReplaceInitCommands, but there is nothing to replace, so we return early.
      return;
    }
    for (InitCommand initCommand : initCommands) {
      addStatementAfterDdlBatch(metadata, initCommand.getInitCommands());
    }
  }

  @Override
  public String[] getSqlDropStrings(Table currentTable, Metadata metadata) {
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
    Iterator<Column> colIterator = table.getColumnIterator();
    while (colIterator.hasNext()) {
      Column col = colIterator.next();
      if (col.isUnique()) {
        String name = Constraint.generateName("UK_", table, col);
        UniqueKey uk = table.getOrCreateUniqueKey(name);
        uk.addColumn(col);
      }
    }
  }
}
