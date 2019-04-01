/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.inheritance;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import java.math.BigDecimal;
import java.util.List;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class TablePerClassTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				DebitAccount.class,
				CreditAccount.class,
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			DebitAccount debitAccount = new DebitAccount();
			debitAccount.setId( 1L );
			debitAccount.setOwner( "John Doe" );
			debitAccount.setBalance( 100  );
			debitAccount.setInterestRate( 1.5);
			debitAccount.setOverdraftFee(  25  );

			CreditAccount creditAccount = new CreditAccount();
			creditAccount.setId( 2L );
			creditAccount.setOwner( "John Doe" );
			creditAccount.setBalance( 1000  );
			creditAccount.setInterestRate(  1.9);
			creditAccount.setCreditLimit(  5000  );

			entityManager.persist( debitAccount );
			entityManager.persist( creditAccount );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::entity-inheritance-table-per-class-query-example[]
			List<Account> accounts = entityManager
				.createQuery( "select a from Account_table_per_class a" )
				.getResultList();
			//end::entity-inheritance-table-per-class-query-example[]
		} );
	}

	//tag::entity-inheritance-table-per-class-example[]
	@Entity(name = "Account_table_per_class")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	private static class Account {

		@Id
		private Long id;

		private String owner;

		private double balance;

		private double interestRate;

		//Getters and setters are omitted for brevity

	//end::entity-inheritance-table-per-class-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getOwner() {
			return owner;
		}

		public void setOwner(String owner) {
			this.owner = owner;
		}

		public double getBalance() {
			return balance;
		}

		public void setBalance(double balance) {
			this.balance = balance;
		}

		public double getInterestRate() {
			return interestRate;
		}

		public void setInterestRate(double interestRate) {
			this.interestRate = interestRate;
		}
	//tag::entity-inheritance-table-per-class-example[]
	}

	@Entity(name = "DebitAccount_table_per_class")
	private static class DebitAccount extends Account {

		private double overdraftFee;

		//Getters and setters are omitted for brevity

	//end::entity-inheritance-table-per-class-example[]

		public double getOverdraftFee() {
			return overdraftFee;
		}

		public void setOverdraftFee(double overdraftFee) {
			this.overdraftFee = overdraftFee;
		}
	//tag::entity-inheritance-table-per-class-example[]
	}

	@Entity(name = "CreditAccount_table_per_class")
	private static class CreditAccount extends Account {

		private double creditLimit;

		//Getters and setters are omitted for brevity

	//end::entity-inheritance-table-per-class-example[]

		public double getCreditLimit() {
			return creditLimit;
		}

		public void setCreditLimit(double creditLimit) {
			this.creditLimit = creditLimit;
		}
	//tag::entity-inheritance-table-per-class-example[]
	}
	//end::entity-inheritance-table-per-class-example[]
}
