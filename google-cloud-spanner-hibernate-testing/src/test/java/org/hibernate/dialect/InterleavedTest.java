/*
 * Copyright 2023 Google LLC
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

package org.hibernate.dialect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.entities.Child;
import org.hibernate.dialect.entities.Child.ChildId;
import org.hibernate.dialect.entities.GrandParent;
import org.hibernate.dialect.entities.Parent;
import org.hibernate.dialect.entities.Parent.ParentId;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

public class InterleavedTest extends BaseEntityManagerFunctionalTestCase {

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class<?>[] {Parent.class, GrandParent.class, Child.class};
  }

  /**
   * We override the default with the 'create-drop' mode because we would like to verify that the
   * interleaved tables are created and dropped in the correct order: parents created before
   * children; children dropped before parents.
   */
  @Override
  protected Map buildSettings() {
    Map settings = super.buildSettings();
    settings.put(org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, "create-drop");
    return settings;
  }

  @Test
  public void testInterleavedEntities() throws SQLException {
    super.buildEntityManagerFactory();

    doInJPA(
        this::entityManagerFactory,
        entityManager -> {
          GrandParent grandParent = new GrandParent();
          grandParent.setName("Grandparent1");
          entityManager.persist(grandParent);

          Parent parent = new Parent();
          parent.setParentId(new ParentId(grandParent.grandParentId, 1L));
          parent.setName("A_Parent");
          entityManager.persist(parent);

          Child child = new Child();
          child.setChildId(new ChildId(parent.parentId, 2L));
          child.setName("Foobar");
          entityManager.persist(child);

          verifyEntities(entityManager, GrandParent.class);
          verifyEntities(entityManager, Parent.class);
          verifyEntities(entityManager, Child.class);
        });

    verifyInterleavedTablesDdl();
  }

  /**
   * This method uses a JDBC connection to query for the tables that exist in the Spanner database
   * and verifies the interleaved relationships are created between tables.
   */
  private static void verifyInterleavedTablesDdl() throws SQLException {
    Configuration configuration = new Configuration();

    try (Connection jdbcConnection =
        DriverManager.getConnection(configuration.getProperty("hibernate.connection.url"))) {
      Statement statement = jdbcConnection.createStatement();
      statement.execute("SELECT * FROM INFORMATION_SCHEMA.TABLES");
      ResultSet tablesResultSet = statement.getResultSet();

      // Interleaved table relationships are stored under the parent_table_name column
      // of information_schema.tables.
      // See: https://cloud.google.com/spanner/docs/information-schema#tables
      HashMap<String, String> childToParentTableMap = new HashMap<>();
      while (tablesResultSet.next()) {
        childToParentTableMap.put(
            tablesResultSet.getString("TABLE_NAME"),
            tablesResultSet.getString("PARENT_TABLE_NAME"));
      }

      assertThat(childToParentTableMap)
          .contains(
              entry("Child", "Parent"), entry("Parent", "GrandParent"), entry("GrandParent", null));
    }
  }

  private static void verifyEntities(EntityManager entityManager, Class<?> entityClass) {
    Session session = entityManager.unwrap(Session.class);
    List<?> entityList =
        session.createQuery("from " + entityClass.getSimpleName(), entityClass).list();
    assertThat(entityList).hasSize(1);
  }
}
