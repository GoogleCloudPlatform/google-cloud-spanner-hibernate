/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.spi.id.inline.InlineIdsOrClauseBulkIdStrategy;

/**
 * @author Daniel Zou
 */
public class NoopIdsClauseBulkStrategy extends InlineIdsOrClauseBulkIdStrategy {

  @Override
  public DeleteHandler buildDeleteHandler(
      SessionFactoryImplementor factory,
      HqlSqlWalker walker) {
    return new NoopBulkStrategyImpl(factory, walker);
  }
}
