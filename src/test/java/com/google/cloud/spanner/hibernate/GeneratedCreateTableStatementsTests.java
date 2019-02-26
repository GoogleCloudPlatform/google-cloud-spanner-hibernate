package com.google.cloud.spanner.hibernate;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.cloud.spanner.hibernate.entities.Employee;
import com.mockrunner.mock.jdbc.JDBCMockObjectFactory;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.Before;
import org.junit.Test;

public class GeneratedCreateTableStatementsTests {

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
        .applySetting("hibernate.hbm2ddl.auto", "create")
        .build();

    this.metadata =
        new MetadataSources(this.registry)
            .addAnnotatedClass(Employee.class)
            .buildMetadata();
  }


  @Test
  public void testUnsupportedExportersNoop() {
    Session session = this.metadata.buildSessionFactory().openSession();
    session.beginTransaction();
    session.close();

    List<String> sqlStrings = this.jdbcMockObjectFactory.getMockConnection()
        .getStatementResultSetHandler()
        .getExecutedStatements();

    assertThat(sqlStrings).containsExactly(
        "create table Employee (id bigint not null,name varchar(255),manager_id bigint) PRIMARY KEY (id)",
        "drop table Employee");
  }


}
