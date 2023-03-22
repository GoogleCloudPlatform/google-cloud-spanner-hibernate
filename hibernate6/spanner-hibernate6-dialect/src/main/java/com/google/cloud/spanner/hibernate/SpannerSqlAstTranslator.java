package com.google.cloud.spanner.hibernate;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.exec.spi.JdbcOperation;

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
