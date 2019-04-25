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
public class HQLCollectionTests extends AbstractHQLTests {

  @Test
  public void test_hql_collection_valued_associations_2() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      Session session = entityManager.unwrap( Session.class );
      String address = "Earth";
      int duration = 20;
      //tag::hql-collection-valued-associations[]

      // alternate syntax
      List<Phone> phones = session.createQuery(
          "select ph " +
              "from Person pr, " +
              "in (pr.phones) ph, " +
              "in (ph.calls) c " +
              "where pr.address = :address " +
              "  and c.duration > :duration" )
          .setParameter( "address", address )
          .setParameter( "duration", duration )
          .list();
      //end::hql-collection-valued-associations[]
      assertEquals(1, phones.size());
      assertEquals( "123-456-7890", phones.get( 0 ).getNumber() );
    });
  }

  @Test
  public void test_hql_collection_expressions_example_1() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      Call call = entityManager.createQuery( "select c from Call c", Call.class).getResultList().get( 1 );
      //tag::hql-collection-expressions-example[]
      List<Phone> phones = entityManager.createQuery(
          "select p " +
              "from Phone p " +
              "where maxelement( p.calls ) = :call", Phone.class )
          .setParameter( "call", call )
          .getResultList();
      //end::hql-collection-expressions-example[]
      assertEquals(1, phones.size());
    });
  }

  @Test
  public void test_hql_collection_expressions_example_2() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      Call call = entityManager.createQuery( "select c from Call c", Call.class).getResultList().get( 0 );
      //tag::hql-collection-expressions-example[]

      List<Phone> phones = entityManager.createQuery(
          "select p " +
              "from Phone p " +
              "where minelement( p.calls ) = :call", Phone.class )
          .setParameter( "call", call )
          .getResultList();
      //end::hql-collection-expressions-example[]
      assertEquals(1, phones.size());
    });
  }

  @Test
  public void test_hql_collection_expressions_example_3() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-collection-expressions-example[]

      List<Person> persons = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where maxindex( p.phones ) = 0", Person.class )
          .getResultList();
      //end::hql-collection-expressions-example[]
      assertEquals(1, persons.size());
    });
  }

  @Test
  public void test_hql_collection_expressions_example_4() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      Call call = entityManager.createQuery( "select c from Call c", Call.class).getResultList().get( 0 );
      Phone phone = call.getPhone();
      //tag::hql-collection-expressions-example[]

      // the above query can be re-written with member of
      List<Person> persons = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where :phone member of p.phones", Person.class )
          .setParameter( "phone", phone )
          .getResultList();
      //end::hql-collection-expressions-example[]
      assertEquals(1, persons.size());
    });
  }

  @Test
  @Ignore
  // Uses SOME keyword. #60
  public void test_hql_collection_expressions_example_5() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      Call call = entityManager.createQuery( "select c from Call c", Call.class).getResultList().get( 0 );
      Phone phone = call.getPhone();
      //tag::hql-collection-expressions-example[]

      List<Person> persons = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where :phone = some elements ( p.phones )", Person.class )
          .setParameter( "phone", phone )
          .getResultList();
      //end::hql-collection-expressions-example[]
      assertEquals(1, persons.size());
    });
  }

  @Test
  public void test_hql_collection_expressions_example_6() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-collection-expressions-example[]

      List<Person> persons = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where exists elements ( p.phones )", Person.class )
          .getResultList();
      //end::hql-collection-expressions-example[]
      assertEquals(2, persons.size());
    });
  }

  @Test
  public void test_hql_collection_expressions_example_7() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-collection-expressions-example[]

      List<Phone> phones = entityManager.createQuery(
          "select p " +
              "from Phone p " +
              "where current_timestamp() > key( p.callHistory )", Phone.class )
          .getResultList();
      //end::hql-collection-expressions-example[]
      assertEquals(2, phones.size());
    });
  }

  @Test
  @Ignore
  // Uses ALL keyword. #59
  public void test_hql_collection_expressions_example_8() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-collection-expressions-example[]

      List<Phone> phones = entityManager.createQuery(
          "select p " +
              "from Phone p " +
              "where current_date() > all elements( p.repairTimestamps )", Phone.class )
          .getResultList();
      //end::hql-collection-expressions-example[]
      assertEquals(3, phones.size());
    });
  }

  @Test
  public void test_hql_collection_expressions_example_9() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-collection-expressions-example[]

      List<Person> persons = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where 1 in indices( p.phones )", Person.class )
          .getResultList();
      //end::hql-collection-expressions-example[]
      assertEquals(1, persons.size());
    });
  }

  @Test
  public void test_hql_collection_expressions_example_10() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-collection-expressions-example[]

      List<Person> persons = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where size( p.phones ) = 2", Person.class )
          .getResultList();
      //end::hql-collection-expressions-example[]
      assertEquals(1, persons.size());
    });
  }

  @Test
  public void test_collection_index_operator_example_1() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-collection-index-operator-example[]
      // indexed lists
      List<Person> persons = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where p.phones[ 0 ].type = 'LAND_LINE'", Person.class )
          .getResultList();
      //end::hql-collection-index-operator-example[]
      assertEquals(1, persons.size());
    });
  }

  @Test
  public void test_hql_collection_index_operator_example_2() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      String address = "Home address";
      //tag::hql-collection-index-operator-example[]

      // maps
      List<Person> persons = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where p.addresses[ 'HOME' ] = :address", Person.class )
          .setParameter( "address", address)
          .getResultList();
      //end::hql-collection-index-operator-example[]
      assertEquals(1, persons.size());
    });
  }

  @Test
  public void test_hql_collection_index_operator_example_3() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-collection-index-operator-example[]

      //max index in list
      List<Person> persons = entityManager.createQuery(
          "select pr " +
              "from Person pr " +
              "where pr.phones[ maxindex(pr.phones) ].type = 'LAND_LINE'", Person.class )
          .getResultList();
      //end::hql-collection-index-operator-example[]
      assertEquals(1, persons.size());
    });
  }

  @Test
  public void test_hql_empty_collection_predicate_example_1() {

    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-empty-collection-predicate-example[]
      List<Person> persons = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where p.phones is empty", Person.class )
          .getResultList();
      //end::hql-empty-collection-predicate-example[]
      assertEquals(1, persons.size());
    });
  }

  @Test
  public void test_hql_empty_collection_predicate_example_2() {

    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-empty-collection-predicate-example[]

      List<Person> persons = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where p.phones is not empty", Person.class )
          .getResultList();
      //end::hql-empty-collection-predicate-example[]
      assertEquals(2, persons.size());
    });
  }

  @Test
  public void test_hql_member_of_collection_predicate_example_1() {

    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-member-of-collection-predicate-example[]
      List<Person> persons = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where 'Home address' member of p.addresses", Person.class )
          .getResultList();
      //end::hql-member-of-collection-predicate-example[]
      assertEquals(1, persons.size());
    });
  }

  @Test
  public void test_hql_member_of_collection_predicate_example_2() {

    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-member-of-collection-predicate-example[]

      List<Person> persons = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "where 'Home address' not member of p.addresses", Person.class )
          .getResultList();
      //end::hql-member-of-collection-predicate-example[]
      assertEquals(2, persons.size());
    });
  }
}
