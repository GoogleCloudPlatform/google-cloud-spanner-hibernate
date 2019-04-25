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
 * This bulk-id strategy inlines identifiers of the rows that need to be updated or deleted using
 * multiple identifier OR clauses.
 *
 * <pre>
 * delete
 * from
 *     Engineer
 * where
 *     (
 *         id = 0
 *         and companyName = 'Red Hat Europe'
 *     )
 *     or (
 *         id = 1
 *       and companyName = 'Red Hat USA'
 *   )
 * </pre>
 *
 * This executes deletes in parallel for faster operation.
 *
 * @author Vlad Mihalcea
 * @author Chengyuan Zhao
 */
public class NoopInlineIdsOrClauseBulkIdStrategy extends InlineIdsOrClauseBulkIdStrategy {

  @Override
  public DeleteHandler buildDeleteHandler(
      SessionFactoryImplementor factory,
      HqlSqlWalker walker) {
    return new NoopDeleteHandlerImpl(factory, walker);
  }
}
