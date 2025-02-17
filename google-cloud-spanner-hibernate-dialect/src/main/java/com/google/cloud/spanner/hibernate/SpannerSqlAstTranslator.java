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

import java.util.List;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.DerivedTableReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.exec.spi.JdbcOperation;

/** We need a translator for the LIKE operator, as Cloud Spanner does not support ESCAPE clauses. */
public class SpannerSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

  // Spanner lacks the lateral keyword and instead has an unnest/array mechanism
  private boolean correlated;

  public SpannerSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
    super(sessionFactory, statement);
  }

  @Override
  public void visitLikePredicate(LikePredicate likePredicate) {
    // Cloud Spanner does not support ESCAPE clauses.
    if (likePredicate.isCaseSensitive()) {
      likePredicate.getMatchExpression().accept(this);
      if (likePredicate.isNegated()) {
        appendSql(" not");
      }
      appendSql(" like ");
      likePredicate.getPattern().accept(this);
    } else {
      if (getDialect().supportsCaseInsensitiveLike()) {
        likePredicate.getMatchExpression().accept(this);
        if (likePredicate.isNegated()) {
          appendSql(" not");
        }
        appendSql(WHITESPACE);
        appendSql(getDialect().getCaseInsensitiveLike());
        appendSql(WHITESPACE);
        likePredicate.getPattern().accept(this);
      } else {
        renderCaseInsensitiveLikeEmulation(
            likePredicate.getMatchExpression(),
            likePredicate.getPattern(),
            null,
            likePredicate.isNegated());
      }
    }
  }

  @Override
  public void visitOffsetFetchClause(QueryPart queryPart) {
    renderLimitOffsetClause(queryPart);
  }

  @Override
  protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
    renderComparisonEmulateIntersect(lhs, operator, rhs);
  }

  @Override
  protected void renderSelectTupleComparison(
      List<SqlSelection> lhsExpressions, SqlTuple tuple, ComparisonOperator operator) {
    emulateSelectTupleComparison(lhsExpressions, tuple.getExpressions(), operator, true);
  }

  @Override
  protected void renderPartitionItem(Expression expression) {
    if (expression instanceof Literal) {
      appendSql("'0' || '0'");
    } else if (expression instanceof Summarization) {
      // This could theoretically be emulated by rendering all grouping variations of the query and
      // connect them via union all but that's probably pretty inefficient and would have to happen
      // on the query spec level
      throw new UnsupportedOperationException("Summarization is not supported by DBMS");
    } else {
      expression.accept(this);
    }
  }

  @Override
  public void visitSelectClause(SelectClause selectClause) {
    getClauseStack().push(Clause.SELECT);

    try {
      appendSql("select ");
      if (correlated) {
        appendSql("as struct ");
      }
      if (selectClause.isDistinct()) {
        appendSql("distinct ");
      }
      visitSqlSelections(selectClause);
    } finally {
      getClauseStack().pop();
    }
  }

  @Override
  protected boolean renderPrimaryTableReference(TableGroup tableGroup, LockMode lockMode) {
    if (shouldInlineCte(tableGroup)) {
      inlineCteTableGroup(tableGroup, lockMode);
      return false;
    }
    final TableReference tableReference = tableGroup.getPrimaryTableReference();
    if (tableReference instanceof NamedTableReference) {
      return renderNamedTableReference((NamedTableReference) tableReference, lockMode);
    }
    final DerivedTableReference derivedTableReference = (DerivedTableReference) tableReference;
    final boolean correlated = derivedTableReference.isLateral();
    final boolean oldCorrelated = this.correlated;
    if (correlated) {
      this.correlated = true;
      appendSql("unnest(array");
    }
    tableReference.accept(this);
    if (correlated) {
      this.correlated = oldCorrelated;
      appendSql(CLOSE_PARENTHESIS);
    }
    return false;
  }

  @Override
  protected boolean supportsRowValueConstructorSyntax() {
    return false;
  }

  @Override
  protected boolean supportsRowValueConstructorSyntaxInInList() {
    return false;
  }

  @Override
  protected boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
    return false;
  }
}
