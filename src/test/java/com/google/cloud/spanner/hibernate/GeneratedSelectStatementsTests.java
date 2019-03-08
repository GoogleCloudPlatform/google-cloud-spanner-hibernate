/*
 * Copyright 2019 Google LLC
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
import com.mockrunner.mock.jdbc.JDBCMockObjectFactory;
import com.mockrunner.mock.jdbc.MockPreparedStatement;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;
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

  /**
   * Set up the metadata for Hibernate to generate schema statements.
   */
  @Before
  public void setup() {
    this.jdbcMockObjectFactory = new JDBCMockObjectFactory();
    this.jdbcMockObjectFactory.registerMockDriver();
    this.jdbcMockObjectFactory.getMockDriver()
        .setupConnection(this.jdbcMockObjectFactory.getMockConnection());

    this.registry = new StandardServiceRegistryBuilder()
        .applySetting("hibernate.dialect", SpannerDialect.class.getName())
        // must NOT set a driver class name so that Hibernate will use java.sql.DriverManager
        // and discover the only mock driver we have set up.
        .applySetting("hibernate.connection.url", "unused")
        .applySetting("hibernate.connection.username", "unused")
        .applySetting("hibernate.connection.password", "unused")
        .build();
    this.metadata =
        new MetadataSources(this.registry).addAnnotatedClass(TestEntity.class)
            .addAnnotatedClass(SubTestEntity.class).buildMetadata();
  }

  @Test
  public void selectLockAcquisitionTest() {
    // the translated statement must NOT show locking statements.
    testStatementTranslation(x -> {
      Query q = x.createQuery("select s from SubTestEntity s");
      q.setLockMode(LockModeType.PESSIMISTIC_READ);
      q.list();
    }, "select subtestent0_.id as id1_0_, subtestent0_.id1 as id2_0_, "
        + "subtestent0_.id2 as id3_0_ from SubTestEntity subtestent0_");
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
      openSessionAndDo(x -> {
        x.save(te);
        x.getTransaction().commit();
      });
    } catch (OptimisticLockException exception) {
      // This exception is expected because the real Transaction is created by the real Session
      // but the mock driver cannot satisfy it.
      String preparedStatement =
          this.jdbcMockObjectFactory.getMockConnection()
              .getPreparedStatementResultSetHandler().getPreparedStatements().stream()
              .map(MockPreparedStatement::getSQL).findFirst().get();
      assertThat(preparedStatement).isEqualTo(
          "insert into `test_table` (`boolColumn`, longVal, "
              + "stringVal, `ID1`, id2) values (?, ?, ?, ?, ?)");
    }
  }

  @Test
  public void deleteDmlTest() {
    testUpdateStatementTranslation(
        "delete TestEntity where boolVal = true",
        "delete from `test_table` where `boolColumn`=TRUE");
  }

  @Test
  public void selectJoinTest() {
    testReadStatementTranslation(
        "select s from SubTestEntity s inner join s.testEntity",
        "select subtestent0_.id as id1_0_, subtestent0_.id1 as id2_0_, "
        + "subtestent0_.id2 as id3_0_ from SubTestEntity subtestent0_ inner join `test_table` "
        + "testentity1_ on subtestent0_.id1=testentity1_.`ID1` "
            + "and subtestent0_.id2=testentity1_.id2");
  }

  private void openSessionAndDo(Consumer<Session> func) {
    Session session = this.metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    func.accept(session);
    session.close();
  }

  private void testStatementTranslation(Consumer<Session> hibernateOperation,
      String executedStatement) {
    openSessionAndDo(hibernateOperation);

    List<String> statements = this.jdbcMockObjectFactory.getMockConnection()
        .getPreparedStatementResultSetHandler().getPreparedStatements().stream()
        .map(MockPreparedStatement::getSQL).collect(
            Collectors.toList());

    assertThat(statements.get(0)).isEqualTo(executedStatement);
  }

  private void testUpdateStatementTranslation(String updateStatement,
      String expectedDatabaseStatement) {
    testStatementTranslation(x -> x.createQuery(updateStatement).executeUpdate(),
        expectedDatabaseStatement);
  }

  private void testReadStatementTranslation(String readStatement,
      String expectedDatabaseStatement) {
    testStatementTranslation(x -> x.createQuery(readStatement).list(), expectedDatabaseStatement);
  }
}
