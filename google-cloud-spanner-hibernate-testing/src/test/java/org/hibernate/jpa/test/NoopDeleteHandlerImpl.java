/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.jpa.test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.spi.id.inline.IdsClauseBuilder;
import org.hibernate.hql.spi.id.inline.InlineIdsIdsOrClauseDeleteHandlerImpl;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * Parallel Inline bulk-id delete handler that uses multiple identifier OR clauses.
 *
 * @author Vlad Mihalcea
 * @author Chengyuan Zhao
 */
public class NoopDeleteHandlerImpl extends InlineIdsIdsOrClauseDeleteHandlerImpl {

  private final List<String> deletes = new ArrayList<>();

  public NoopDeleteHandlerImpl(
      SessionFactoryImplementor factory,
      HqlSqlWalker walker) {
    super(factory, walker);
  }

  @Override
  public String[] getSqlStatements() {
    return deletes.toArray(new String[deletes.size()]);
  }

  @Override
  public int execute(
      SharedSessionContractImplementor session,
      QueryParameters queryParameters) {

  	return 0;
  }

}
