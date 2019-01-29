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

import static org.junit.Assert.assertEquals;

import com.google.cloud.spanner.hibernate.util.SubTestEntity;
import com.google.cloud.spanner.hibernate.util.TestEntity;
import com.mockrunner.mock.jdbc.JDBCMockObjectFactory;
import com.mockrunner.mock.jdbc.MockPreparedStatement;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * These tests use SpannerDialect and Hibernate-core to generate the final SELECT statements that
 * are sent to the Spanner driver.
 *
 * @author Chengyuan
 */
public class GeneratedSelectStatementsTests {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

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
  public void deleteDmlTest() {
    testStatementTranslation(
        x -> x.createQuery("delete TestEntity where boolVal = true").executeUpdate(),
        "delete from test_table where boolVal=TRUE");
  }

  @Test
  public void selectJoinTest() {
    testStatementTranslation(
        x -> x.createQuery("select s from SubTestEntity s inner join s.testEntity").list(),
        "select subtestent0_.id as id1_0_, subtestent0_.id1 as id2_0_, "
        + "subtestent0_.id2 as id3_0_ from SubTestEntity subtestent0_ inner join test_table "
        + "testentity1_ on subtestent0_.id1=testentity1_.id1 "
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

    assertEquals(executedStatement, statements.get(0));
  }
}
