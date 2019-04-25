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
public class HQLJoinTests extends AbstractHQLTests {

  @Test
  public void test_hql_explicit_inner_join_example_1() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-explicit-inner-join-example[]
      List<Person> persons = entityManager.createQuery(
          "select distinct pr " +
              "from Person pr " +
              "join pr.phones ph " +
              "where ph.type = :phoneType", Person.class )
          .setParameter( "phoneType", PhoneType.MOBILE )
          .getResultList();
      //end::hql-explicit-inner-join-example[]
      assertEquals(1, persons.size());
    });
  }

  @Test
  public void test_hql_explicit_inner_join_example_2() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-explicit-inner-join-example[]

      // same query but specifying join type as 'inner' explicitly
      List<Person> persons = entityManager.createQuery(
          "select distinct pr " +
              "from Person pr " +
              "inner join pr.phones ph " +
              "where ph.type = :phoneType", Person.class )
          .setParameter( "phoneType", PhoneType.MOBILE )
          .getResultList();
      //end::hql-explicit-inner-join-example[]
      assertEquals(1, persons.size());
    });
  }

  @Test
  public void test_hql_explicit_outer_join_example_1() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-explicit-outer-join-example[]
      List<Person> persons = entityManager.createQuery(
          "select distinct pr " +
              "from Person pr " +
              "left join pr.phones ph " +
              "where ph is null " +
              "   or ph.type = :phoneType", Person.class )
          .setParameter( "phoneType", PhoneType.LAND_LINE )
          .getResultList();
      //end::hql-explicit-outer-join-example[]
      assertEquals(2, persons.size());
    });
  }

  @Test
  public void test_hql_explicit_outer_join_example_2() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-explicit-outer-join-example[]

      // functionally the same query but using the 'left outer' phrase
      List<Person> persons = entityManager.createQuery(
          "select distinct pr " +
              "from Person pr " +
              "left outer join pr.phones ph " +
              "where ph is null " +
              "   or ph.type = :phoneType", Person.class )
          .setParameter( "phoneType", PhoneType.LAND_LINE )
          .getResultList();
      //end::hql-explicit-outer-join-example[]
      assertEquals(2, persons.size());
    });
  }

  @Test
  public void test_hql_explicit_fetch_join_example() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-explicit-fetch-join-example[]

      // functionally the same query but using the 'left outer' phrase
      List<Person> persons = entityManager.createQuery(
          "select distinct pr " +
              "from Person pr " +
              "left join fetch pr.phones ", Person.class )
          .getResultList();
      //end::hql-explicit-fetch-join-example[]
      assertEquals(3, persons.size());
    });
  }

  @Test
  public void test_hql_explicit_join_with_example() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      Session session = entityManager.unwrap( Session.class );
      //tag::hql-explicit-join-with-example[]
      List<Object[]> personsAndPhones = session.createQuery(
          "select pr.name, ph.number " +
              "from Person pr " +
              "left join pr.phones ph with ph.type = :phoneType " )
          .setParameter( "phoneType", PhoneType.LAND_LINE )
          .list();
      //end::hql-explicit-join-with-example[]
      assertEquals(4, personsAndPhones.size());
    });
  }

  @Test
  public void test_jpql_explicit_join_on_example() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-explicit-join-jpql-on-example[]
      List<Object[]> personsAndPhones = entityManager.createQuery(
          "select pr.name, ph.number " +
              "from Person pr " +
              "left join pr.phones ph on ph.type = :phoneType " )
          .setParameter( "phoneType", PhoneType.LAND_LINE )
          .getResultList();
      //end::hql-explicit-join-jpql-on-example[]
      assertEquals(4, personsAndPhones.size());
    });
  }

  @Test
  public void test_hql_implicit_join_example_1() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      String address = "Earth";
      //tag::hql-implicit-join-example[]
      List<Phone> phones = entityManager.createQuery(
          "select ph " +
              "from Phone ph " +
              "where ph.person.address = :address ", Phone.class )
          .setParameter( "address", address )
          .getResultList();
      //end::hql-implicit-join-example[]
      assertEquals(3, phones.size());
    });
  }

  @Test
  public void test_hql_implicit_join_example_2() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      String address = "Earth";
      //tag::hql-implicit-join-example[]

      // same as
      List<Phone> phones = entityManager.createQuery(
          "select ph " +
              "from Phone ph " +
              "join ph.person pr " +
              "where pr.address = :address ", Phone.class )
          .setParameter( "address", address)
          .getResultList();
      //end::hql-implicit-join-example[]
      assertEquals(3, phones.size());
    });
  }

  @Test
  public void test_hql_implicit_join_alias_example_1() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      String address = "Earth";
      Date timestamp = new Date(0);
      //tag::hql-implicit-join-alias-example[]
      List<Phone> phones = entityManager.createQuery(
          "select ph " +
              "from Phone ph " +
              "where ph.person.address = :address " +
              "  and ph.person.createdOn > :timestamp", Phone.class )
          .setParameter( "address", address )
          .setParameter( "timestamp", timestamp )
          .getResultList();
      //end::hql-implicit-join-alias-example[]
      assertEquals(3, phones.size());
    });
  }

  @Test
  public void test_hql_implicit_join_alias_example_2() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      String address = "Earth";
      Date timestamp = new Date(0);
      //tag::hql-implicit-join-alias-example[]

      //same as
      List<Phone> phones = entityManager.createQuery(
          "select ph " +
              "from Phone ph " +
              "inner join ph.person pr " +
              "where pr.address = :address " +
              "  and pr.createdOn > :timestamp", Phone.class )
          .setParameter( "address", address )
          .setParameter( "timestamp", timestamp )
          .getResultList();
      //end::hql-implicit-join-alias-example[]
      assertEquals(3, phones.size());
    });
  }

  @Test
  public void test_hql_collection_valued_associations_1() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      String address = "Earth";
      int duration = 20;
      //tag::hql-collection-valued-associations[]
      List<Phone> phones = entityManager.createQuery(
          "select ph " +
              "from Person pr " +
              "join pr.phones ph " +
              "join ph.calls c " +
              "where pr.address = :address " +
              "  and c.duration > :duration", Phone.class )
          .setParameter( "address", address )
          .setParameter( "duration", duration )
          .getResultList();
      //end::hql-collection-valued-associations[]
      assertEquals(1, phones.size());
      assertEquals( "123-456-7890", phones.get( 0 ).getNumber() );
    });

  }

  @Test
  public void test_hql_collection_qualification_associations_1() {

    doInJPA( this::entityManagerFactory, entityManager -> {
      Long id = 1L;
      //tag::hql-collection-qualification-example[]

      // select all the calls (the map value) for a given Phone
      List<Call> calls = entityManager.createQuery(
          "select ch " +
              "from Phone ph " +
              "join ph.callHistory ch " +
              "where ph.id = :id ", Call.class )
          .setParameter( "id", id )
          .getResultList();
      //end::hql-collection-qualification-example[]
      assertEquals(2, calls.size());
    });

  }

  @Test
  public void test_hql_collection_qualification_associations_2() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      Long id = 1L;
      //tag::hql-collection-qualification-example[]

      // same as above
      List<Call> calls = entityManager.createQuery(
          "select value(ch) " +
              "from Phone ph " +
              "join ph.callHistory ch " +
              "where ph.id = :id ", Call.class )
          .setParameter( "id", id )
          .getResultList();
      //end::hql-collection-qualification-example[]
      assertEquals(2, calls.size());
    });
  }

  @Test
  public void test_hql_collection_qualification_associations_3() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      Long id = 1L;
      //tag::hql-collection-qualification-example[]

      // select all the Call timestamps (the map key) for a given Phone
      List<Date> timestamps = entityManager.createQuery(
          "select key(ch) " +
              "from Phone ph " +
              "join ph.callHistory ch " +
              "where ph.id = :id ", Date.class )
          .setParameter( "id", id )
          .getResultList();
      //end::hql-collection-qualification-example[]
      assertEquals(2, timestamps.size());
    });
  }

  @Test
  public void test_hql_collection_qualification_associations_4() {
    try {
      doInJPA( this::entityManagerFactory, entityManager -> {

        Long id = 1L;
        //tag::hql-collection-qualification-example[]

        // select all the Call and their timestamps (the 'Map.Entry') for a given Phone
        List<Map.Entry<Date, Call>> callHistory = entityManager.createQuery(
            "select entry(ch) " +
                "from Phone ph " +
                "join ph.callHistory ch " +
                "where ph.id = :id " )
            .setParameter( "id", id )
            .getResultList();
        //end::hql-collection-qualification-example[]

      });
    } catch(Exception e) {
      //@see https://hibernate.atlassian.net/browse/HHH-10491
    }
  }

  @Test
  public void test_hql_collection_qualification_associations_5() {
    doInJPA( this::entityManagerFactory, entityManager -> {

      Long id = 1L;
      Integer phoneIndex = 0;
      //tag::hql-collection-qualification-example[]

      // Sum all call durations for a given Phone of a specific Person
      Long duration = entityManager.createQuery(
          "select sum(ch.duration) " +
              "from Person pr " +
              "join pr.phones ph " +
              "join ph.callHistory ch " +
              "where ph.id = :id " +
              "  and index(ph) = :phoneIndex", Long.class )
          .setParameter( "id", id )
          .setParameter( "phoneIndex", phoneIndex )
          .getSingleResult();
      //end::hql-collection-qualification-example[]
      assertEquals(45, duration.intValue());

    });
  }

  @Test
  public void test_hql_numeric_arithmetic_example_1() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-numeric-arithmetic-example[]
      // select clause date/time arithmetic operations
      Long duration = entityManager.createQuery(
          "select sum(ch.duration) * :multiplier " +
              "from Person pr " +
              "join pr.phones ph " +
              "join ph.callHistory ch " +
              "where ph.id = 1L ", Long.class )
          .setParameter( "multiplier", 1000L )
          .getSingleResult();
      //end::hql-numeric-arithmetic-example[]
      assertTrue(duration > 0);
    });
  }

  @Test
  public void test_hql_aggregate_functions_example_3() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-aggregate-functions-example[]

      List<Object[]> callCount = entityManager.createQuery(
          "select p.number, count(c) " +
              "from Call c " +
              "join c.phone p " +
              "group by p.number", Object[].class )
          .getResultList();
      //end::hql-aggregate-functions-example[]
      assertNotNull(callCount.get( 0 ));
    });
  }

  @Test
  public void test_hql_concat_function_example() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-concat-function-example[]
      List<String> callHistory = entityManager.createQuery(
          "select concat( p.number, ' : ' , cast(c.duration as string) ) " +
              "from Call c " +
              "join c.phone p", String.class )
          .getResultList();
      //end::hql-concat-function-example[]
      assertEquals(2, callHistory.size());
    });
  }

  @Test
  public void test_hql_substring_function_example() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-substring-function-example[]
      List<String> prefixes = entityManager.createQuery(
          "select substring( p.number, 1, 2 ) " +
              "from Call c " +
              "join c.phone p", String.class )
          .getResultList();
      //end::hql-substring-function-example[]
      assertEquals(2, prefixes.size());
    });
  }

  @Test
  public void test_hql_select_clause_dynamic_list_instantiation_example() {
    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-select-clause-dynamic-list-instantiation-example[]
      List<List> phoneCallDurations = entityManager.createQuery(
          "select new list(" +
              "	p.number, " +
              "	c.duration " +
              ")  " +
              "from Call c " +
              "join c.phone p ", List.class )
          .getResultList();
      //end::hql-select-clause-dynamic-list-instantiation-example[]
      assertNotNull(phoneCallDurations);
    });
  }

  @Test
  public void test_hql_select_clause_dynamic_map_instantiation_example() {

    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-select-clause-dynamic-map-instantiation-example[]
      List<Map> phoneCallTotalDurations = entityManager.createQuery(
          "select new map(" +
              "	p.number as phoneNumber , " +
              "	sum(c.duration) as totalDuration, " +
              "	avg(c.duration) as averageDuration " +
              ")  " +
              "from Call c " +
              "join c.phone p " +
              "group by p.number ", Map.class )
          .getResultList();
      //end::hql-select-clause-dynamic-map-instantiation-example[]
      assertNotNull(phoneCallTotalDurations);
    });
  }

  @Test
  @Ignore
  // Uses ALL keyword. #59
  public void test_hql_all_subquery_comparison_qualifier_example() {

    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-all-subquery-comparison-qualifier-example[]
      // select all persons with all calls shorter than 50 seconds
      List<Person> persons = entityManager.createQuery(
          "select distinct p.person " +
              "from Phone p " +
              "join p.calls c " +
              "where 50 > all ( " +
              "	select duration" +
              "	from Call" +
              "	where phone = p " +
              ") ", Person.class )
          .getResultList();
      //end::hql-all-subquery-comparison-qualifier-example[]
      assertEquals(1, persons.size());
    });
  }

  @Test
  public void test_hql_between_predicate_example_1() {

    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-between-predicate-example[]
      List<Person> persons = entityManager.createQuery(
          "select p " +
              "from Person p " +
              "join p.phones ph " +
              "where p.id = (select min(id) from Person) and index(ph) between 0 and 3", Person.class )
          .getResultList();
      //end::hql-between-predicate-example[]
      assertEquals(1, persons.size());
    });
  }

  @Test
  public void test_hql_group_by_example_2() {

    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-group-by-example[]

      List<Object[]> personTotalCallDurations = entityManager.createQuery(
          "select p.name, sum( c.duration ) " +
              "from Call c " +
              "join c.phone ph " +
              "join ph.person p " +
              "group by p.name", Object[].class )
          .getResultList();
      //end::hql-group-by-example[]
      assertEquals(1, personTotalCallDurations.size());
    });
  }

  @Test
  @Ignore
  // Failed to group by entity. #56
  public void test_hql_group_by_example_3() {

    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-group-by-example[]

      //It's even possible to group by entities!
      List<Object[]> personTotalCallDurations = entityManager.createQuery(
          "select p, sum( c.duration ) " +
              "from Call c " +
              "join c.phone ph " +
              "join ph.person p " +
              "group by p", Object[].class )
          .getResultList();
      //end::hql-group-by-example[]
      assertEquals(1, personTotalCallDurations.size());
    });
  }

  @Test
  @Ignore
  // Failed to group by entity. #56
  public void test_hql_group_by_example_4() {

    doInJPA( this::entityManagerFactory, entityManager -> {

      Call call11 = new Call();
      call11.setDuration( 10 );
      call11.setTimestamp( Timestamp.from( LocalDateTime.of( 2000, 1, 1, 0, 0, 0 ).toInstant( ZoneOffset.UTC ) ) );

      Phone phone = entityManager.createQuery( "select p from Phone p where p.calls is empty ", Phone.class).getResultList().get( 0 );

      phone.addCall(call11);
      entityManager.flush();
      entityManager.clear();

      List<Object[]> personTotalCallDurations = entityManager.createQuery(
          "select p, sum( c.duration ) " +
              "from Call c " +
              "join c.phone p " +
              "group by p", Object[].class )
          .getResultList();
      assertEquals(2, personTotalCallDurations.size());
    });
  }

  @Test
  public void test_hql_group_by_having_example() {

    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-group-by-having-example[]

      List<Object[]> personTotalCallDurations = entityManager.createQuery(
          "select p.name, sum( c.duration ) " +
              "from Call c " +
              "join c.phone ph " +
              "join ph.person p " +
              "group by p.name " +
              "having sum( c.duration ) > 1000", Object[].class )
          .getResultList();
      //end::hql-group-by-having-example[]
      assertEquals(0, personTotalCallDurations.size());
    });
  }

  @Test
  public void test_hql_order_by_example_2() {

    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-order-by-example[]

      List<Object[]> personTotalCallDurations = entityManager.createQuery(
          "select p.name, sum( c.duration ) as total " +
              "from Call c " +
              "join c.phone ph " +
              "join ph.person p " +
              "group by p.name " +
              "order by total", Object[].class )
          .getResultList();
      //end::hql-order-by-example[]
      assertEquals(1, personTotalCallDurations.size());
    });
  }

  @Test
  public void test_hql_read_only_entities_example() {

    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-read-only-entities-example[]
      List<Call> calls = entityManager.createQuery(
          "select c " +
              "from Call c " +
              "join c.phone p " +
              "where p.number = :phoneNumber ", Call.class )
          .setParameter( "phoneNumber", "123-456-7890" )
          .setHint( "org.hibernate.readOnly", true )
          .getResultList();

      calls.forEach( c -> c.setDuration( 0 ) );
      //end::hql-read-only-entities-example[]
    });
  }

  @Test
  public void test_hql_read_only_entities_native_example() {

    doInJPA( this::entityManagerFactory, entityManager -> {
      //tag::hql-read-only-entities-native-example[]
      List<Call> calls = entityManager.createQuery(
          "select c " +
              "from Call c " +
              "join c.phone p " +
              "where p.number = :phoneNumber ", Call.class )
          .setParameter( "phoneNumber", "123-456-7890" )
          .unwrap( org.hibernate.query.Query.class )
          .setReadOnly( true )
          .getResultList();
      //end::hql-read-only-entities-native-example[]
    });
  }
}
