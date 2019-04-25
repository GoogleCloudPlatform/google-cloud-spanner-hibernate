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
public class BasicHQLTests extends AbstractHQLTests {

	@Test
	public void test_hql_select_simplest_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
						Session session = entityManager.unwrap( Session.class );
			List<Object> objects = session.createQuery(
				"from java.lang.Object" )
			.list();

			//tag::hql-select-simplest-example[]
			List<Person> persons = session.createQuery(
				"from Person" )
			.list();
			//end::hql-select-simplest-example[]
		});
	}

	@Test
	public void test_hql_api_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			//tag::hql-api-example[]
			org.hibernate.query.Query query = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name"
			);
			//end::hql-api-example[]
		});
	}

	@Test
	public void test_hql_api_named_query_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			//tag::hql-api-named-query-example[]
			org.hibernate.query.Query query = session.getNamedQuery( "get_person_by_name" );
			//end::hql-api-named-query-example[]
		});
	}

	@Test
	public void test_hql_api_basic_usage_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			//tag::hql-api-basic-usage-example[]
			org.hibernate.query.Query query = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name" )
			// timeout - in seconds
			.setTimeout( 2 )
			// write to L2 caches, but do not read from them
			.setCacheMode( CacheMode.REFRESH )
			// assuming query cache was enabled for the SessionFactory
			.setCacheable( true )
			// add a comment to the generated SQL if enabled via the hibernate.use_sql_comments configuration property
			.setComment( "+ INDEX(p idx_person_name)" );
			//end::hql-api-basic-usage-example[]
		});
	}

	@Test
	public void test_hql_api_parameter_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			//tag::hql-api-parameter-example[]
			org.hibernate.query.Query query = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name" )
			.setParameter( "name", "J%", StringType.INSTANCE );
			//end::hql-api-parameter-example[]
		});
	}

	@Test
	public void test_hql_api_parameter_inferred_type_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			//tag::hql-api-parameter-inferred-type-example[]
			org.hibernate.query.Query query = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name" )
			.setParameter( "name", "J%" );
			//end::hql-api-parameter-inferred-type-example[]
		});
	}

	@Test
	public void test_hql_api_parameter_short_form_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Date timestamp = new Date(  );
			Session session = entityManager.unwrap( Session.class );
			//tag::hql-api-parameter-short-form-example[]
			org.hibernate.query.Query query = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name " +
				"  and p.createdOn > :timestamp" )
			.setParameter( "name", "J%" )
			.setParameter( "timestamp", timestamp, TemporalType.TIMESTAMP);
			//end::hql-api-parameter-short-form-example[]
		});
	}

	@Test(expected = IllegalArgumentException.class)
	public void test_hql_api_positional_parameter_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Date timestamp = new Date(  );
			Session session = entityManager.unwrap( Session.class );
			//tag::hql-api-positional-parameter-example[]
			org.hibernate.query.Query query = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like ?" )
			.setParameter( 1, "J%" );
			//end::hql-api-positional-parameter-example[]
		});
	}

	@Test
	public void test_hql_api_list_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			//tag::hql-api-list-example[]
			List<Person> persons = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name" )
			.setParameter( "name", "J%" )
			.list();
			//end::hql-api-list-example[]
		});
	}

	@Test
	public void test_hql_api_stream_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			//tag::hql-api-stream-example[]
			try( Stream<Person> persons = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name" )
			.setParameter( "name", "J%" )
			.stream() ) {

				Map<Phone, List<Call>> callRegistry = persons
						.flatMap( person -> person.getPhones().stream() )
						.flatMap( phone -> phone.getCalls().stream() )
						.collect( Collectors.groupingBy( Call::getPhone ) );

				process(callRegistry);
			}
			//end::hql-api-stream-example[]
		});
	}

	private void process(Map<Phone, List<Call>> callRegistry) {
	}

	@Test
	public void test_hql_api_stream_projection_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			//tag::hql-api-stream-projection-example[]
			try ( Stream<Object[]> persons = session.createQuery(
				"select p.name, p.nickName " +
				"from Person p " +
				"where p.name like :name" )
			.setParameter( "name", "J%" )
			.stream() ) {

				persons
				.map( row -> new PersonNames(
						(String) row[0],
						(String) row[1] ) )
				.forEach( this::process );
			}
			//end::hql-api-stream-projection-example[]
		});
	}

	@Test
	public void test_hql_api_scroll_projection_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			//tag::hql-api-scroll-example[]
			try ( ScrollableResults scrollableResults = session.createQuery(
					"select p " +
					"from Person p " +
					"where p.name like :name" )
					.setParameter( "name", "J%" )
					.scroll()
			) {
				while(scrollableResults.next()) {
					Person person = (Person) scrollableResults.get()[0];
					process(person);
				}
			}
			//end::hql-api-scroll-example[]
		});
	}

	@Test
	public void test_hql_api_scroll_open_example() {
		ScrollableResults scrollableResults = doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			return session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name" )
			.setParameter( "name", "J%" )
			.scroll();
		});
		try {
			scrollableResults.next();
			fail("Should throw exception because the ResultSet must be closed by now!");
		}
		catch ( Exception expected ) {
		}
	}

	private void process(Person person) {
	}

	private void process(PersonNames personName) {
	}

	@Test
	public void test_hql_api_unique_result_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			//tag::hql-api-unique-result-example[]
			Person person = (Person) session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name" )
			.setParameter( "name", "J%" )
			.uniqueResult();
			//end::hql-api-unique-result-example[]
		});
	}

	@Test
	public void test_hql_string_literals_example_1() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-string-literals-example[]
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like 'Joe'", Person.class)
			.getResultList();
			//end::hql-string-literals-example[]
		});
	}

	@Test
	@Ignore
	// #55
	// TODO: Test fails due to quote literals in string: 'Joe''s'; figure out if there
	// is a way to enhance dialect to modify escaping.
	public void test_hql_string_literals_example_2() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-string-literals-example[]

			// Escaping quotes
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like 'Joe''s'", Person.class)
			.getResultList();
			//end::hql-string-literals-example[]
		});
	}

	@Test
	public void test_hql_string_literals_example_3() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-numeric-literals-example[]
			// simple integer literal
			Person person = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.id = (select min(id) from Person) ", Person.class)
			.getSingleResult();
			//end::hql-numeric-literals-example[]
		});
	}

	@Test
	public void test_hql_string_literals_example_4() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-numeric-literals-example[]

			// simple integer literal, typed as a long
			Person person = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.id = (select min(id) from Person) ", Person.class)
			.getSingleResult();
			//end::hql-numeric-literals-example[]
		});
	}

	@Test
	public void test_hql_string_literals_example_5() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-numeric-literals-example[]

			// decimal notation
			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"where c.duration > 100.5", Call.class )
			.getResultList();
			//end::hql-numeric-literals-example[]
		});
	}

	@Test
	public void test_hql_string_literals_example_6() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-numeric-literals-example[]

			// decimal notation, typed as a float
			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"where c.duration > 100.5F", Call.class )
			.getResultList();
			//end::hql-numeric-literals-example[]
		});
	}

	@Test
	public void test_hql_string_literals_example_7() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-numeric-literals-example[]

			// scientific notation
			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"where c.duration > 1e+2", Call.class )
			.getResultList();
			//end::hql-numeric-literals-example[]
		});
	}

	@Test
	public void test_hql_string_literals_example_8() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-numeric-literals-example[]

			// scientific notation, typed as a float
			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"where c.duration > 1e+2F", Call.class )
			.getResultList();
			//end::hql-numeric-literals-example[]
		});

	}

	@Test
	public void test_hql_numeric_arithmetic_example_2() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-numeric-arithmetic-example[]

			// select clause date/time arithmetic operations
			Integer years = entityManager.createQuery(
				"select year( current_date() ) - year( p.createdOn ) " +
				"from Person p " +
				"where p.id = (select min(id) from Person) ", Integer.class )
			.getSingleResult();
			//end::hql-numeric-arithmetic-example[]
			assertTrue(years > 0);
		});
	}

	@Test
	public void test_hql_numeric_arithmetic_example_3() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-numeric-arithmetic-example[]

			// where clause arithmetic operations
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where year( current_date() ) - year( p.createdOn ) > 1", Person.class )
			.getResultList();
			//end::hql-numeric-arithmetic-example[]
			assertTrue(persons.size() > 0);
		});
	}

	@Test
	public void test_hql_concatenation_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-concatenation-example[]
			String name = entityManager.createQuery(
				"select 'Customer ' || p.name " +
				"from Person p " +
				"where p.id = (select min(id) from Person) ", String.class )
			.getSingleResult();
			//end::hql-concatenation-example[]
			assertNotNull(name);
		});
	}

	@Test
	public void test_hql_aggregate_functions_example_1() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-aggregate-functions-example[]
			Object[] callStatistics = entityManager.createQuery(
				"select " +
				"	count(c), " +
				"	sum(c.duration), " +
				"	min(c.duration), " +
				"	max(c.duration), " +
				"	avg(c.duration)  " +
				"from Call c ", Object[].class )
			.getSingleResult();
			//end::hql-aggregate-functions-example[]
			assertNotNull(callStatistics);
		});
	}

	@Test
	public void test_hql_aggregate_functions_example_2() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-aggregate-functions-example[]

			Long phoneCount = entityManager.createQuery(
				"select count( distinct c.phone ) " +
				"from Call c ", Long.class )
			.getSingleResult();
			//end::hql-aggregate-functions-example[]
			assertNotNull(phoneCount);
		});
	}

	@Test
	public void test_hql_upper_function_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-upper-function-example[]
			List<String> names = entityManager.createQuery(
				"select upper( p.name ) " +
				"from Person p ", String.class )
			.getResultList();
			//end::hql-upper-function-example[]
			assertEquals(3, names.size());
		});
	}

	@Test
	public void test_hql_lower_function_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-lower-function-example[]
			List<String> names = entityManager.createQuery(
				"select lower( p.name ) " +
				"from Person p ", String.class )
			.getResultList();
			//end::hql-lower-function-example[]
			assertEquals(3, names.size());
		});
	}

	@Test
	public void test_hql_trim_function_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-trim-function-example[]
			List<String> names = entityManager.createQuery(
				"select trim( p.name ) " +
				"from Person p ", String.class )
			.getResultList();
			//end::hql-trim-function-example[]
			assertEquals(3, names.size());
		});
	}

	@Test
	public void test_hql_length_function_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-length-function-example[]
			List<Long> lengths = entityManager.createQuery(
				"select length( p.name ) " +
				"from Person p ", Long.class )
			.getResultList();
			//end::hql-length-function-example[]
			assertEquals(3, lengths.size());
		});
	}

	@Test
	public void test_hql_locate_function_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-locate-function-example[]
			List<Long> sizes = entityManager.createQuery(
				"select locate( 'John', p.name ) " +
				"from Person p ", Long.class )
			.getResultList();
			//end::hql-locate-function-example[]
			assertEquals(3, sizes.size());
		});
	}

	@Test
	public void test_hql_abs_function_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-abs-function-example[]
			List<Integer> abs = entityManager.createQuery(
				"select abs( c.duration ) " +
				"from Call c ", Integer.class )
			.getResultList();
			//end::hql-abs-function-example[]
			assertEquals(2, abs.size());
		});
	}

	@Test
	public void test_hql_mod_function_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-mod-function-example[]
			List<Long> mods = entityManager.createQuery(
				"select mod( c.duration, 10 ) " +
				"from Call c ", Long.class )
			.getResultList();
			//end::hql-mod-function-example[]
			assertEquals(2, mods.size());
		});
	}

	@Test
	public void test_hql_sqrt_function_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-sqrt-function-example[]
			List<Double> sqrts = entityManager.createQuery(
				"select sqrt( c.duration ) " +
				"from Call c ", Double.class )
			.getResultList();
			//end::hql-sqrt-function-example[]
			assertEquals(2, sqrts.size());
		});
	}

	@Test
	public void test_hql_current_date_function_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-current-date-function-example[]
			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"where c.timestamp = current_timestamp", Call.class )
			.getResultList();
			//end::hql-current-date-function-example[]
			assertEquals(0, calls.size());
		});
	}

	@Test
	public void test_hql_current_time_function_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-current-time-function-example[]
			List<Call> calls = entityManager.createQuery(
					"select c " +
							"from Call c " +
							"where c.timestamp = current_time()", Call.class )
					.getResultList();
			//end::hql-current-time-function-example[]
			assertEquals(0, calls.size());
		});
	}

	@Test
	public void test_hql_current_timestamp_function_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-current-timestamp-function-example[]
			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"where c.timestamp = current_timestamp", Call.class )
			.getResultList();
			//end::hql-current-timestamp-function-example[]
			assertEquals(0, calls.size());
		});
	}

	@Test
	@Ignore
	// Uses bit_length function. #62
	public void test_hql_bit_length_function_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-bit-length-function-example[]
			List<Number> bits = entityManager.createQuery(
					"select bit_length( c.duration ) " +
							"from Call c ", Number.class )
					.getResultList();
			//end::hql-bit-length-function-example[]
			assertEquals(2, bits.size());
		});
	}

	@Test
	public void test_hql_cast_function_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-cast-function-example[]
			List<String> durations = entityManager.createQuery(
				"select cast( c.duration as string ) " +
				"from Call c ", String.class )
			.getResultList();
			//end::hql-cast-function-example[]
			assertEquals(2, durations.size());
		});
	}

	@Test
	public void test_hql_extract_function_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-extract-function-example[]
			List<Long> years = entityManager.createQuery(
				"select extract( YEAR from c.timestamp ) " +
				"from Call c ", Long.class )
			.getResultList();
			//end::hql-extract-function-example[]
			assertEquals(2, years.size());
		});
	}

	@Test
	public void test_hql_year_function_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-year-function-example[]
			List<Integer> years = entityManager.createQuery(
				"select year( c.timestamp ) " +
				"from Call c ", Integer.class )
			.getResultList();
			//end::hql-year-function-example[]
			assertEquals(2, years.size());
		});
	}

	@Test
	@SkipForDialect(SQLServerDialect.class)
	public void test_hql_str_function_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-str-function-example[]
			List<String> timestamps = entityManager.createQuery(
				"select str( c.timestamp ) " +
				"from Call c ", String.class )
			.getResultList();
			//end::hql-str-function-example[]
			assertEquals(2, timestamps.size());
		});
	}

	@Test
	public void test_hql_str_function_example_sql_server() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-str-function-example[]
			List<String> timestamps = entityManager.createQuery(
				"select str( cast(duration as float) / 60, 4, 2 ) " +
				"from Call c ", String.class )
			.getResultList();
			//end::hql-str-function-example[]
			assertEquals(2, timestamps.size());
		});
	}

	@Test
	public void test_hql_polymorphism_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-polymorphism-example[]
			List<Payment> payments = entityManager.createQuery(
				"select p " +
				"from Payment p ", Payment.class )
			.getResultList();
			//end::hql-polymorphism-example[]
			assertEquals(2, payments.size());
		});
	}

	@Test
	public void test_hql_entity_type_exp_example_1() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-entity-type-exp-example[]
			List<Payment> payments = entityManager.createQuery(
				"select p " +
				"from Payment p " +
				"where type(p) = CreditCardPayment", Payment.class )
			.getResultList();
			//end::hql-entity-type-exp-example[]
			assertEquals(1, payments.size());
		});
	}

	@Test
	public void test_hql_entity_type_exp_example_2() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-entity-type-exp-example[]
			List<Payment> payments = entityManager.createQuery(
				"select p " +
				"from Payment p " +
				"where type(p) = :type", Payment.class )
			.setParameter( "type", WireTransferPayment.class)
			.getResultList();
			//end::hql-entity-type-exp-example[]
			assertEquals(1, payments.size());
		});
	}

	@Test
	public void test_simple_case_expressions_example_1() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-simple-case-expressions-example[]
			List<String> nickNames = entityManager.createQuery(
				"select " +
				"	case p.nickName " +
				"	when 'NA' " +
				"	then '<no nick name>' " +
				"	else p.nickName " +
				"	end " +
				"from Person p", String.class )
			.getResultList();
			//end::hql-simple-case-expressions-example[]
			assertEquals(3, nickNames.size());
		});
	}

	@Test
	public void test_simple_case_expressions_example_2() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-simple-case-expressions-example[]

			// same as above
			List<String> nickNames = entityManager.createQuery(
				"select coalesce(p.nickName, '<no nick name>') " +
				"from Person p", String.class )
			.getResultList();
			//end::hql-simple-case-expressions-example[]
			assertEquals(3, nickNames.size());
		});
	}

	@Test
	public void test_searched_case_expressions_example_1() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-searched-case-expressions-example[]
			List<String> nickNames = entityManager.createQuery(
				"select " +
				"	case " +
				"	when p.nickName is null " +
				"	then " +
				"		case " +
				"		when p.name is null " +
				"		then '<no nick name>' " +
				"		else p.name " +
				"		end" +
				"	else p.nickName " +
				"	end " +
				"from Person p", String.class )
			.getResultList();
			//end::hql-searched-case-expressions-example[]
			assertEquals(3, nickNames.size());
		});
	}

	@Test
	public void test_searched_case_expressions_example_2() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-searched-case-expressions-example[]

			// coalesce can handle this more succinctly
			List<String> nickNames = entityManager.createQuery(
				"select coalesce( p.nickName, p.name, '<no nick name>' ) " +
				"from Person p", String.class )
			.getResultList();
			//end::hql-searched-case-expressions-example[]
			assertEquals(3, nickNames.size());
		});
	}

	/**
	 * This test depends on the sequence-table's autogenerated IDs starting at 1 each time,
	 * but because our Hibernate mode is "update" instead of "create-drop" we do not
	 * reset the auto-generated IDs.
	 */
	@Test
	@Ignore
	public void test_case_arithmetic_expressions_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-case-arithmetic-expressions-example[]
			List<Long> values = entityManager.createQuery(
				"select " +
				"	case when p.nickName is null " +
				"		 then (p.id * 1000) " +
				"		 else p.id " +
				"	end " +
				"from Person p " +
				"order by p.id", Long.class)
			.getResultList();

			assertEquals(3, values.size());
			assertEquals( 1L, (long) values.get( 0 ) );
			assertEquals( 2000, (long) values.get( 1 ) );
			assertEquals( 3000, (long) values.get( 2 ) );
			//end::hql-case-arithmetic-expressions-example[]
		});
	}

	@Test
	public void test_hql_null_if_example_1() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-nullif-example[]
			List<String> nickNames = entityManager.createQuery(
				"select nullif( p.nickName, p.name ) " +
				"from Person p", String.class )
			.getResultList();
			//end::hql-nullif-example[]
			assertEquals(3, nickNames.size());
		});
	}

	@Test
	public void test_hql_null_if_example_2() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-nullif-example[]

			// equivalent CASE expression
			List<String> nickNames = entityManager.createQuery(
				"select " +
				"	case" +
				"	when p.nickName = p.name" +
				"	then null" +
				"	else p.nickName" +
				"	end " +
				"from Person p", String.class )
			.getResultList();
			//end::hql-nullif-example[]
			assertEquals(3, nickNames.size());
		});
	}

	@Test
	public void test_hql_select_clause_dynamic_instantiation_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-select-clause-dynamic-instantiation-example[]
			CallStatistics callStatistics = entityManager.createQuery(
				"select new org.hibernate.userguide.hql.CallStatistics(" +
				"	count(c), " +
				"	sum(c.duration), " +
				"	min(c.duration), " +
				"	max(c.duration), " +
				"	avg(c.duration)" +
				")  " +
				"from Call c ", CallStatistics.class )
			.getSingleResult();
			//end::hql-select-clause-dynamic-instantiation-example[]
			assertNotNull(callStatistics);
		});
	}

	@Test
	public void test_hql_relational_comparisons_example_1() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-relational-comparisons-example[]
			// numeric comparison
			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"where c.duration < 30 ", Call.class )
			.getResultList();
			//end::hql-relational-comparisons-example[]
			assertEquals(1, calls.size());
		});
	}

	@Test
	public void test_hql_relational_comparisons_example_2() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-relational-comparisons-example[]

			// string comparison
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like 'John%' ", Person.class )
			.getResultList();
			//end::hql-relational-comparisons-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_relational_comparisons_example_3() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-relational-comparisons-example[]

			// datetime comparison
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.createdOn > '1950-01-01' ", Person.class )
			.getResultList();
			//end::hql-relational-comparisons-example[]
			assertEquals(2, persons.size());
		});
	}

	@Test
	public void test_hql_relational_comparisons_example_4() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-relational-comparisons-example[]

			// enum comparison
			List<Phone> phones = entityManager.createQuery(
				"select p " +
				"from Phone p " +
				"where p.type = 'MOBILE' ", Phone.class )
			.getResultList();
			//end::hql-relational-comparisons-example[]
			assertEquals(1, phones.size());
		});
	}

	@Test
	public void test_hql_relational_comparisons_example_5() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-relational-comparisons-example[]

			// boolean comparison
			List<Payment> payments = entityManager.createQuery(
				"select p " +
				"from Payment p " +
				"where p.completed = true ", Payment.class )
			.getResultList();
			//end::hql-relational-comparisons-example[]
			assertEquals(2, payments.size());
		});
	}

	@Test
	public void test_hql_relational_comparisons_example_6() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-relational-comparisons-example[]

			// boolean comparison
			List<Payment> payments = entityManager.createQuery(
				"select p " +
				"from Payment p " +
				"where type(p) = WireTransferPayment ", Payment.class )
			.getResultList();
			//end::hql-relational-comparisons-example[]
			assertEquals(1, payments.size());
		});
	}

	@Test
	public void test_hql_relational_comparisons_example_7() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-relational-comparisons-example[]

			// entity value comparison
			List<Object[]> phonePayments = entityManager.createQuery(
				"select p " +
				"from Payment p, Phone ph " +
				"where p.person = ph.person ", Object[].class )
			.getResultList();
			//end::hql-relational-comparisons-example[]
			assertEquals(3, phonePayments.size());
		});
	}

	@Test
	public void test_hql_null_predicate_example_1() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-null-predicate-example[]
			// select all persons with a nickname
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.nickName is not null", Person.class )
			.getResultList();
			//end::hql-null-predicate-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_null_predicate_example_2() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-null-predicate-example[]

			// select all persons without a nickname
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.nickName is null", Person.class )
			.getResultList();
			//end::hql-null-predicate-example[]
			assertEquals(2, persons.size());
		});
	}

	@Test
	public void test_hql_like_predicate_example_1() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-like-predicate-example[]
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like 'Jo%'", Person.class )
			.getResultList();
			//end::hql-like-predicate-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_like_predicate_example_2() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-like-predicate-example[]

			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name not like 'Jo%'", Person.class )
			.getResultList();
			//end::hql-like-predicate-example[]
			assertEquals(2, persons.size());
		});
	}

	@Test
	@Ignore
	// #61
	// Uses LIKE + ESCAPE; Spanner does not support specifying custom escape character.
	public void test_hql_like_predicate_escape_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-like-predicate-escape-example[]
			// find any person with a name starting with "Dr_"
			List<Person> persons = entityManager.createQuery(
					"select p " +
							"from Person p " +
							"where p.name like 'Dr|_%' escape '|'", Person.class )
					.getResultList();
			//end::hql-like-predicate-escape-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_between_predicate_example_2() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-between-predicate-example[]

			List<Person> persons = entityManager.createQuery(
					"select p " +
							"from Person p " +
							"where p.createdOn between '1999-01-01' and '2001-01-02'", Person.class )
					.getResultList();
			//end::hql-between-predicate-example[]
			assertEquals(2, persons.size());
		});
	}

	@Test
	public void test_hql_between_predicate_example_3() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-between-predicate-example[]

			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"where c.duration between 5 and 20", Call.class )
			.getResultList();
			//end::hql-between-predicate-example[]
			assertEquals(1, calls.size());
		});
	}

	@Test
	public void test_hql_between_predicate_example_4() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-between-predicate-example[]

			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name between 'H' and 'M'", Person.class )
			.getResultList();
			//end::hql-between-predicate-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_in_predicate_example_1() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-in-predicate-example[]
			List<Payment> payments = entityManager.createQuery(
				"select p " +
				"from Payment p " +
				"where type(p) in ( CreditCardPayment, WireTransferPayment )", Payment.class )
			.getResultList();
			//end::hql-in-predicate-example[]
			assertEquals(2, payments.size());
		});
	}

	@Test
	public void test_hql_in_predicate_example_2() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-in-predicate-example[]

			List<Phone> phones = entityManager.createQuery(
				"select p " +
				"from Phone p " +
				"where type in ( 'MOBILE', 'LAND_LINE' )", Phone.class )
			.getResultList();
			//end::hql-in-predicate-example[]
			assertEquals(3, phones.size());
		});
	}

	@Test
	public void test_hql_in_predicate_example_3() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-in-predicate-example[]

			List<Phone> phones = entityManager.createQuery(
				"select p " +
				"from Phone p " +
				"where type in :types", Phone.class )
			.setParameter( "types", Arrays.asList( PhoneType.MOBILE, PhoneType.LAND_LINE ) )
			.getResultList();
			//end::hql-in-predicate-example[]
			assertEquals(3, phones.size());
		});
	}

	@Test
	public void test_hql_in_predicate_example_4() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-in-predicate-example[]

			List<Phone> phones = entityManager.createQuery(
				"select distinct p " +
				"from Phone p " +
				"where p.person.id in (" +
				"	select py.person.id " +
				"	from Payment py" +
				"	where py.completed = true and py.amount > 50 " +
				")", Phone.class )
			.getResultList();
			//end::hql-in-predicate-example[]
			assertEquals(2, phones.size());
		});
	}

	@Test
	public void test_hql_in_predicate_example_5() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-in-predicate-example[]

			// Not JPQL compliant!
			List<Phone> phones = entityManager.createQuery(
				"select distinct p " +
				"from Phone p " +
				"where p.person in (" +
				"	select py.person " +
				"	from Payment py" +
				"	where py.completed = true and py.amount > 50 " +
				")", Phone.class )
			.getResultList();
			//end::hql-in-predicate-example[]
			assertEquals(2, phones.size());
		});
	}


	@Test
	public void test_hql_in_predicate_example_6() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-in-predicate-example[]

			// Not JPQL compliant!
			List<Payment> payments = entityManager.createQuery(
				"select distinct p " +
				"from Payment p " +
				"where ( p.amount, p.completed ) in (" +
				"	(50, true )," +
				"	(100, true )," +
				"	(5, false )" +
				")", Payment.class )
			.getResultList();
			//end::hql-in-predicate-example[]
			assertEquals(1, payments.size());
		});
	}

	@Test
	public void test_hql_group_by_example_1() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-group-by-example[]
			Long totalDuration = entityManager.createQuery(
				"select sum( c.duration ) " +
				"from Call c ", Long.class )
			.getSingleResult();
			//end::hql-group-by-example[]
			assertEquals(Long.valueOf( 45 ), totalDuration);
		});
	}

	@Test
	public void test_hql_order_by_example_1() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-order-by-example[]
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"order by p.name", Person.class )
			.getResultList();
			//end::hql-order-by-example[]
			assertEquals(3, persons.size());
		});
	}
}
