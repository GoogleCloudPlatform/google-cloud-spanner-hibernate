/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import javax.persistence.*;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class CalendarWithTemporalTimestampTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				DateEvent.class
		};
	}

	@Test
	public void testLifecycle() {
		final Calendar calendar = new GregorianCalendar();
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( new DateEvent( calendar ) );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			DateEvent dateEvent = entityManager.createQuery( "from "
					+ "DateEvent_calendar_with_temporal_timestamp", DateEvent.class ).getSingleResult();
			//Assert.assertEquals( calendar, dateEvent.getTimestamp() );
		} );
	}

	@Entity(name = "DateEvent_calendar_with_temporal_timestamp")
	private static class DateEvent {

		@Id
		@GeneratedValue
		private Long id;

		@Temporal(TemporalType.TIMESTAMP)
		@Column(name = "`timestamp`")
		private Calendar timestamp;

		public DateEvent() {
		}

		public DateEvent(Calendar timestamp) {
			this.timestamp = timestamp;
		}

		public Long getId() {
			return id;
		}

		public Calendar getTimestamp() {
			return timestamp;
		}
	}
}
