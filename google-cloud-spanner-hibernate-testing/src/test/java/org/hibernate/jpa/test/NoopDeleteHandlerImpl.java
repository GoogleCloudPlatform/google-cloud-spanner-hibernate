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
public class NoopDeleteHandlerImpl extends
    InlineIdsIdsOrClauseDeleteHandlerImpl {

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

    IdsClauseBuilder values = prepareInlineStatement(session, queryParameters);

    if (!values.getIds().isEmpty()) {
      final String idSubselect = values.toStatement();

      for (Type type : getTargetedQueryable().getPropertyTypes()) {
        if (type.isCollectionType()) {
          CollectionType cType = (CollectionType) type;
          AbstractCollectionPersister cPersister = (AbstractCollectionPersister) factory()
              .getMetamodel().collectionPersister(cType.getRole());
          if (cPersister.isManyToMany()) {
            deletes.add(generateDelete(
                cPersister.getTableName(),
                cPersister.getKeyColumnNames(),
                idSubselect,
                "bulk delete - m2m join table cleanup"
            ).toStatementString());
          }
        }
      }

      String[] tableNames = getTargetedQueryable().getConstraintOrderedTableNameClosure();
      String[][] columnNames = getTargetedQueryable().getContraintOrderedTableKeyColumnClosure();
      for (int i = 0; i < tableNames.length; i++) {
        // TODO : an optimization here would be to consider cascade deletes and not gen those delete statements;
        //      the difficulty is the ordering of the tables here vs the cascade attributes on the persisters ->
        //          the table info gotten here should really be self-contained (i.e., a class representation
        //          defining all the needed attributes), then we could then get an array of those
        deletes.add(generateDelete(tableNames[i], columnNames[i], idSubselect, "bulk delete")
            .toStatementString());
      }

      // Start performing the deletes
      deletes.parallelStream().forEach(delete -> {
        if (delete == null) {
          return;
        }

        try {
          try (PreparedStatement ps = session
              .getJdbcCoordinator().getStatementPreparer()
              .prepareStatement(delete, false)) {
            session
                .getJdbcCoordinator().getResultSetReturn()
                .executeUpdate(ps);
          }
        } catch (SQLException e) {
          throw convert(e, "error performing bulk delete", delete);
        }
      });
    }

    return values.getIds().size();
  }

}