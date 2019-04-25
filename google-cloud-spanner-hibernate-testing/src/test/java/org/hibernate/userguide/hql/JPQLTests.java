/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.hql;

import org.hibernate.CacheMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.dialect.*;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.type.StringType;
import org.hibernate.userguide.model.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.persistence.FlushModeType;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assumptions.assumeThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class JPQLTests extends AbstractHQLTests {

  @Test
  public void test_hql_select_simplest_jpql_example() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-select-simplest-jpql-example[]
      List<Person> persons = entityManager.createQuery(
          "select p " +
              "from Person p", Person.class )
          .getResultList();
      //end::hql-select-simplest-jpql-example[]
    });
  }

  @Test
  public void hql_select_simplest_jpql_fqn_example() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-select-simplest-jpql-fqn-example[]
      List<Person> persons = entityManager.createQuery(
          "select p " +
              "from org.hibernate.userguide.model.Person p", Person.class )
          .getResultList();
      //end::hql-select-simplest-jpql-fqn-example[]
    });
  }

  @Test
  public void test_hql_multiple_root_reference_jpql_example() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-multiple-root-reference-jpql-example[]
      List<Object[]> persons = entityManager.createQuery(
          "select distinct pr, ph " +
              "from Person pr, Phone ph " +
              "where ph.person = pr and ph is not null", Object[].class)
          .getResultList();
      //end::hql-multiple-root-reference-jpql-example[]
      assertEquals(3, persons.size());
    });
  }

  @Test
  public void test_hql_multiple_same_root_reference_jpql_example() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-multiple-same-root-reference-jpql-example[]
      List<Person> persons = entityManager.createQuery(
          "select distinct pr1 " +
              "from Person pr1, Person pr2 " +
              "where pr1.id <> pr2.id " +
              "  and pr1.address = pr2.address " +
              "  and pr1.createdOn < pr2.createdOn", Person.class )
          .getResultList();
      //end::hql-multiple-same-root-reference-jpql-example[]
      assertEquals(1, persons.size());
    });
  }

  @Test
  public void test_jpql_api_example() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::jpql-api-example[]
      Query query = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where p.name like :name"
      );

      TypedQuery<Person> typedQuery = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where p.name like :name", Person.class
      );
      //end::jpql-api-example[]
    });
  }

  @Test
  public void test_jpql_api_named_query_example() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::jpql-api-named-query-example[]
      Query query = entityManager.createNamedQuery( "get_person_by_name" );

      TypedQuery<Person> typedQuery = entityManager.createNamedQuery(
          "get_person_by_name", Person.class
      );
      //end::jpql-api-named-query-example[]
    });
  }

  @Test
  public void test_jpql_api_hibernate_named_query_example() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::jpql-api-hibernate-named-query-example[]
      Phone phone = entityManager
          .createNamedQuery( "get_phone_by_number", Phone.class )
          .setParameter( "number", "123-456-7890" )
          .getSingleResult();
      //end::jpql-api-hibernate-named-query-example[]
      assertNotNull( phone );
    });
  }

  @Test
  public void test_jpql_api_basic_usage_example() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::jpql-api-basic-usage-example[]
      Query query = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where p.name like :name" )
          // timeout - in milliseconds
          .setHint( "javax.persistence.query.timeout", 2000 )
          // flush only at commit time
          .setFlushMode( FlushModeType.COMMIT );
      //end::jpql-api-basic-usage-example[]
    });
  }

  @Test
  public void test_jpql_api_parameter_example_1() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::jpql-api-parameter-example[]
      Query query = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where p.name like :name" )
          .setParameter( "name", "J%" );
      //end::jpql-api-parameter-example[]
    });
  }

  @Test
  public void test_jpql_api_parameter_example_2() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      Date timestamp = new Date(  );
      //tag::jpql-api-parameter-example[]

      // For generic temporal field types (e.g. `java.util.Date`, `java.util.Calendar`)
      // we also need to provide the associated `TemporalType`
      Query query = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where p.createdOn > :timestamp" )
          .setParameter( "timestamp", timestamp, TemporalType.DATE );
      //end::jpql-api-parameter-example[]
    });
  }

  @Test
  public void test_jpql_api_positional_parameter_example() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::jpql-api-positional-parameter-example[]
      Query query = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where p.name like ?1" )
          .setParameter( 1, "J%" );
      //end::jpql-api-positional-parameter-example[]
    });
  }

  @Test
  public void test_jpql_api_list_example() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::jpql-api-list-example[]
      List<Person> persons = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where p.name like :name" )
          .setParameter( "name", "J%" )
          .getResultList();
      //end::jpql-api-list-example[]
    });
  }

  @Test
  public void test_jpql_api_single_result_example() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::jpql-api-single-result-example[]
      Person person = (Person) entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where p.name like :name" )
          .setParameter( "name", "J%" )
          .getSingleResult();
      //end::jpql-api-single-result-example[]
    });
  }
}
