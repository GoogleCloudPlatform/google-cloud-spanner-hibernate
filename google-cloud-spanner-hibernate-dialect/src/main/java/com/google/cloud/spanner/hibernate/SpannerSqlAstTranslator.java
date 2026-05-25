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

import org.hibernate.Locking;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * SQL AST translator for Cloud Spanner.
 *
 * @param <T> type of JDBC operation.
 */
public class SpannerSqlAstTranslator<T extends JdbcOperation>
    extends org.hibernate.dialect.sql.ast.SpannerSqlAstTranslator<T> {

  public SpannerSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
    super(sessionFactory, statement);
  }

  @Override
  protected LockStrategy determineLockingStrategy(
      QuerySpec querySpec, Locking.FollowOn followOnStrategy) {
    if (followOnStrategy == Locking.FollowOn.FORCE) {
      return LockStrategy.FOLLOW_ON;
    }

    if (!querySpec.isRoot()) {
      followOnStrategy = Locking.FollowOn.ALLOW;
    }

    LockStrategy strategy = LockStrategy.CLAUSE;

    if (!querySpec.getGroupByClauseExpressions().isEmpty()) {
      if (followOnStrategy == Locking.FollowOn.DISALLOW) {
        throw new IllegalQueryOperationException("Locking with GROUP BY is not supported");
      } else if (followOnStrategy == Locking.FollowOn.IGNORE) {
        return LockStrategy.NONE;
      }
      strategy = LockStrategy.FOLLOW_ON;
    }

    if (querySpec.getHavingClauseRestrictions() != null) {
      if (followOnStrategy == Locking.FollowOn.DISALLOW) {
        throw new IllegalQueryOperationException("Locking with HAVING is not supported");
      } else if (followOnStrategy == Locking.FollowOn.IGNORE) {
        return LockStrategy.NONE;
      }
      strategy = LockStrategy.FOLLOW_ON;
    }

    if (querySpec.getSelectClause().isDistinct()) {
      if (followOnStrategy == Locking.FollowOn.DISALLOW) {
        throw new IllegalQueryOperationException("Locking with DISTINCT is not supported");
      } else if (followOnStrategy == Locking.FollowOn.IGNORE) {
        return LockStrategy.NONE;
      }
      strategy = LockStrategy.FOLLOW_ON;
    }

    if (!getDialect().supportsOuterJoinForUpdate()) {
      LockingClauseStrategy lockingClauseStrategy = getLockingClauseStrategy();
      if (lockingClauseStrategy != null && lockingClauseStrategy.containsOuterJoins()) {
        // we have any outer joins to lock, but the dialect does not support locking outer joins
        // 		-we need to use follow-on locking if allowed
        if (followOnStrategy == Locking.FollowOn.DISALLOW) {
          throw new IllegalQueryOperationException("Locking with OUTER joins is not supported");
        } else if (followOnStrategy == Locking.FollowOn.IGNORE) {
          return LockStrategy.NONE;
        }
        strategy = LockStrategy.FOLLOW_ON;
      }
    }

    if (hasAggregateFunctions(querySpec)) {
      if (followOnStrategy == Locking.FollowOn.DISALLOW) {
        throw new IllegalQueryOperationException(
            "Locking with aggregate functions is not supported");
      } else if (followOnStrategy == Locking.FollowOn.IGNORE) {
        return LockStrategy.NONE;
      }
      strategy = LockStrategy.FOLLOW_ON;
    }

    return strategy;
  }
}
