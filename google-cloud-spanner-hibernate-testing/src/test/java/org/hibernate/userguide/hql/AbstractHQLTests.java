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
public abstract class AbstractHQLTests extends BaseEntityManagerFunctionalTestCase {

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class<?>[] {
        Person.class,
        Phone.class,
        Call.class,
        CreditCardPayment.class,
        WireTransferPayment.class
    };
  }

  @Before
  public void init() {
    doInJPA(this::entityManagerFactory, entityManager -> {
      Person person1 = new Person("John Doe");
      person1.setNickName("JD");
      person1.setAddress("Earth");
      person1.setCreatedOn(Timestamp.from(LocalDateTime.of(2000, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC)));
      person1.getAddresses().put(AddressType.HOME, "Home address");
      person1.getAddresses().put(AddressType.OFFICE, "Office address");
      entityManager.persist(person1);

      Person person2 = new Person("Mrs. John Doe");
      person2.setAddress("Earth");
      person2.setCreatedOn(Timestamp.from(LocalDateTime.of(2000, 1, 2, 12, 0, 0).toInstant(ZoneOffset.UTC)));
      entityManager.persist(person2);

      Person person3 = new Person("Dr_ John Doe");
      entityManager.persist(person3);

      Phone phone1 = new Phone("123-456-7890");
      phone1.setId(1L);
      phone1.setType(PhoneType.MOBILE);
      person1.addPhone(phone1);
      phone1.getRepairTimestamps().add(Timestamp.from(LocalDateTime.of(2005, 1, 1, 12, 0, 0).toInstant(ZoneOffset.UTC)));
      phone1.getRepairTimestamps().add(Timestamp.from(LocalDateTime.of(2006, 1, 1, 12, 0, 0).toInstant(ZoneOffset.UTC)));

      Call call11 = new Call();
      call11.setDuration(12);
      call11.setTimestamp(Timestamp.from(LocalDateTime.of(2000, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC)));

      Call call12 = new Call();
      call12.setDuration(33);
      call12.setTimestamp(Timestamp.from(LocalDateTime.of(2000, 1, 1, 1, 0, 0).toInstant(ZoneOffset.UTC)));

      phone1.addCall(call11);
      phone1.addCall(call12);

      Phone phone2 = new Phone("098-765-4321");
      phone2.setId(2L);
      phone2.setType(PhoneType.LAND_LINE);

      Phone phone3 = new Phone("098-765-4320");
      phone3.setId(3L);
      phone3.setType(PhoneType.LAND_LINE);

      person2.addPhone(phone2);
      person2.addPhone(phone3);

      CreditCardPayment creditCardPayment = new CreditCardPayment();
      creditCardPayment.setCompleted(true);
      creditCardPayment.setAmount(0L);
      creditCardPayment.setPerson(person1);

      WireTransferPayment wireTransferPayment = new WireTransferPayment();
      wireTransferPayment.setCompleted(true);
      wireTransferPayment.setAmount(100L);
      wireTransferPayment.setPerson(person2);

      entityManager.persist(creditCardPayment);
      entityManager.persist(wireTransferPayment);
    });
  }
}
