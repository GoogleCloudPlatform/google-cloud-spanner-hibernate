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
public class SingleTableTest extends BaseEntityManagerFunctionalTestCase {

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
			//tag::entity-inheritance-single-table-persist-example[]
			DebitAccount debitAccount = new DebitAccount();
			debitAccount.setId( 1L );
			debitAccount.setOwner( "John Doe" );
			debitAccount.setBalance( 100  );
			debitAccount.setInterestRate(  1.5d );
			debitAccount.setOverdraftFee( 25 );

			CreditAccount creditAccount = new CreditAccount();
			creditAccount.setId( 2L );
			creditAccount.setOwner( "John Doe" );
			creditAccount.setBalance(  1000 );
			creditAccount.setInterestRate( 1.9  );
			creditAccount.setCreditLimit( 5000  );

			entityManager.persist( debitAccount );
			entityManager.persist( creditAccount );
			//end::entity-inheritance-single-table-persist-example[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::entity-inheritance-single-table-query-example[]
			List<Account> accounts = entityManager
				.createQuery( "select a from Account_single_table a" )
				.getResultList();
			//end::entity-inheritance-single-table-query-example[]
		} );
	}

	//tag::entity-inheritance-single-table-example[]
	@Entity(name = "Account_single_table")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	private static class Account {

		@Id
		private Long id;

		private String owner;

		private double balance;

		private double interestRate;

		//Getters and setters are omitted for brevity

	//end::entity-inheritance-single-table-example[]

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
	//tag::entity-inheritance-single-table-example[]
	}

	@Entity(name = "DebitAccount_single_table")
	private static class DebitAccount extends Account {

		private double overdraftFee;

		//Getters and setters are omitted for brevity

	//end::entity-inheritance-single-table-example[]

		public double getOverdraftFee() {
			return overdraftFee;
		}

		public void setOverdraftFee(double overdraftFee) {
			this.overdraftFee = overdraftFee;
		}
	//tag::entity-inheritance-single-table-example[]
	}

	@Entity(name = "CreditAccount_single_table")
	private static class CreditAccount extends Account {

		private double creditLimit;

		//Getters and setters are omitted for brevity

	//end::entity-inheritance-single-table-example[]

		public double getCreditLimit() {
			return creditLimit;
		}

		public void setCreditLimit(double creditLimit) {
			this.creditLimit = creditLimit;
		}
	//tag::entity-inheritance-single-table-example[]
	}
	//end::entity-inheritance-single-table-example[]
}
