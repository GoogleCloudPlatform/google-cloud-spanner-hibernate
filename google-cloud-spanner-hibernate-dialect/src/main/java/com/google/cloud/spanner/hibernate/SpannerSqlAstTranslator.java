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

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.exec.spi.JdbcOperation;

/** We need a translator for the LIKE operator, as Cloud Spanner does not support ESCAPE clauses. */
public class SpannerSqlAstTranslator<T extends JdbcOperation>
    extends org.hibernate.dialect.SpannerSqlAstTranslator<T> {

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
}