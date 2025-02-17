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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.cloud.spanner.hibernate.entities.SubTestEntity;
import com.google.cloud.spanner.hibernate.entities.TestEntity;
import com.google.cloud.spanner.hibernate.entities.TestEntity.IdClass;
import com.google.common.collect.ImmutableList;
import com.mockrunner.mock.jdbc.JDBCMockObjectFactory;
import com.mockrunner.mock.jdbc.MockConnection;
import com.mockrunner.mock.jdbc.MockPreparedStatement;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.query.Query;
import org.junit.Before;
import org.junit.Test;

/**
 * These tests use SpannerDialect and Hibernate-core to generate the final SELECT statements that
 * are sent to the Spanner driver.
 *
 * @author Chengyuan Zhao
 */
public class GeneratedSelectStatementsTests {

  private Metadata metadata;

  private StandardServiceRegistry registry;

  private JDBCMockObjectFactory jdbcMockObjectFactory;

  /** Set up the metadata for Hibernate to generate schema statements. */
  @Before
  public void setup() throws SQLException {
    this.jdbcMockObjectFactory = new JDBCMockObjectFactory();
    this.jdbcMockObjectFactory.registerMockDriver();

    MockConnection connection = this.jdbcMockObjectFactory.getMockConnection();
    connection.setMetaData(MockJdbcUtils.metaDataBuilder().build());
    this.jdbcMockObjectFactory.getMockDriver().setupConnection(connection);

    this.registry =
        new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", SpannerDialect.class.getName())
            // must NOT set a driver class name so that Hibernate will use java.sql.DriverManager
            // and discover the only mock driver we have set up.
            .applySetting("hibernate.connection.url", "unused")
            .applySetting("hibernate.connection.username", "unused")
            .applySetting("hibernate.connection.password", "unused")
            .applySetting("hibernate.hbm2ddl.auto", "create")
            .build();

    this.metadata =
        new MetadataSources(this.registry)
            .addAnnotatedClass(TestEntity.class)
            .addAnnotatedClass(SubTestEntity.class)
            .buildMetadata();
  }

  @Test
  public void selectLockAcquisitionTest() {
    // the translated statement should include an 'for update' clause.
    testStatementTranslation(
        x -> {
          Query<?> q =
              x.createQuery("select s from SubTestEntity s").setFirstResult(8).setMaxResults(15);
          q.setLockMode(LockModeType.PESSIMISTIC_READ);
          q.list();
        },
        "select ste1_0.id,ste1_0.id1,ste1_0.id2 from SubTestEntity ste1_0 limit ? offset ? for update");
  }

  @Test
  public void insertDmlTest() {
    TestEntity te = new TestEntity();
    IdClass id = new IdClass();
    id.id2 = "A";
    id.id1 = 1L;
    te.id = id;
    te.stringVal = "asdf";
    try {
      openSessionAndDo(
          x -> {
            x.save(te);
            x.getTransaction().commit();
          });
    } catch (OptimisticLockException exception) {
      // This exception is expected because the real Transaction is created by the real Session
      // but the mock driver cannot satisfy it.
      String preparedStatement =
          this.jdbcMockObjectFactory.getMockConnection().getPreparedStatementResultSetHandler()
              .getPreparedStatements().stream()
              .map(MockPreparedStatement::getSQL)
              .findFirst()
              .get();
      assertThat(preparedStatement)
          .isEqualTo(
              "insert into `test_table` (`boolColumn`,floatVal,floatValStoredAsDouble,longVal,stringVal,`ID1`,id2) "
                  + "values (?,?,?,?,?,?,?)");
    }
  }

  @Test
  public void limitOffsetClauseTest() {
    openSessionAndDo(
        session -> {
          Query q =
              session
                  .createQuery("select s from SubTestEntity s")
                  .setFirstResult(8)
                  .setMaxResults(15);
          q.list();
        });

    MockPreparedStatement statement =
        this.jdbcMockObjectFactory
            .getMockConnection()
            .getPreparedStatementResultSetHandler()
            .getPreparedStatements()
            .get(0);

    assertThat(statement.getSQL())
        .isEqualTo(
            "select ste1_0.id,ste1_0.id1,ste1_0.id2 from SubTestEntity ste1_0 limit ? offset ?");
    assertThat(statement.getParameter(1)).isEqualTo(15);
    assertThat(statement.getParameter(2)).isEqualTo(8);
  }

  @Test
  public void deleteDmlTest() {
    testUpdateStatementTranslation(
        "delete TestEntity where boolVal = true",
        ImmutableList.of(
            "delete from `TestEntity_stringList` where exists "
                + "(select 1 from `test_table` te1_0 where "
                + "(te1_0.`ID1`=`TestEntity_stringList`.`TestEntity_ID1` "
                + "and te1_0.id2=`TestEntity_stringList`.`TestEntity_id2`) "
                + "and (te1_0.`boolColumn`=true))",
            "delete from `test_table` where `boolColumn`=true"));
  }

  @Test
  public void selectJoinTest() {
    testReadStatementTranslation(
        "select s from SubTestEntity s inner join s.testEntity",
        "select ste1_0.id,ste1_0.id1,ste1_0.id2 from SubTestEntity ste1_0 "
            + "join `test_table` te1_0 on te1_0.`ID1`=ste1_0.id1 and te1_0.id2=ste1_0.id2");
  }

  private void openSessionAndDo(Consumer<Session> func) {
    Session session = this.metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    func.accept(session);
    session.close();
  }

  private void testStatementTranslation(
      Consumer<Session> hibernateOperation, String executedStatement) {
    testStatementTranslation(hibernateOperation, ImmutableList.of(executedStatement));
  }

  private void testStatementTranslation(
      Consumer<Session> hibernateOperation, List<String> executedStatement) {
    openSessionAndDo(hibernateOperation);

    List<String> statements =
        this.jdbcMockObjectFactory.getMockConnection().getPreparedStatementResultSetHandler()
            .getPreparedStatements().stream()
            .map(MockPreparedStatement::getSQL)
            .collect(Collectors.toList());

    assertThat(statements).isEqualTo(executedStatement);
  }

  private void testUpdateStatementTranslation(
      String updateStatement, String expectedDatabaseStatement) {
    testUpdateStatementTranslation(updateStatement, ImmutableList.of(expectedDatabaseStatement));
  }

  private void testUpdateStatementTranslation(
      String updateStatement, List<String> expectedDatabaseStatement) {
    testStatementTranslation(
        x -> x.createQuery(updateStatement).executeUpdate(), expectedDatabaseStatement);
  }

  private void testReadStatementTranslation(
      String readStatement, String expectedDatabaseStatement) {
    testStatementTranslation(x -> x.createQuery(readStatement).list(), expectedDatabaseStatement);
  }
}
