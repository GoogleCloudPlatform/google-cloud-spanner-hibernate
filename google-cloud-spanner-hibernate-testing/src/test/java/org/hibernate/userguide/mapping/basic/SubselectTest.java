/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic;

import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import javax.persistence.*;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class SubselectTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Client.class,
			Account.class,
			AccountTransaction.class,
			AccountSummary.class
		};
	}

	@Test
	public void testLifecycle() {
		//tag::mapping-Subselect-entity-find-example[]
		doInJPA( this::entityManagerFactory, entityManager -> {
			Client client = new Client();
			client.setId( 1L );
			client.setFirstName( "John" );
			client.setLastName( "Doe" );
			entityManager.persist( client );

			Account account = new Account();
			account.setId( 1L );
			account.setClient( client );
			account.setDescription( "Checking account" );
			entityManager.persist( account );

			AccountTransaction transaction = new AccountTransaction();
			transaction.setAccount( account );
			transaction.setDescription( "Salary" );
			transaction.setCents( 100 * 7000 );
			entityManager.persist( transaction );

			AccountSummary summary = entityManager.createQuery(
				"select s " +
				"from AccountSummary s " +
				"where s.id = :id", AccountSummary.class)
			.setParameter( "id", account.getId() )
			.getSingleResult();

			assertEquals( "John Doe", summary.getClientName() );
			assertEquals( 100 * 7000, summary.getBalance() );
		} );
		//end::mapping-Subselect-entity-find-example[]

		//tag::mapping-Subselect-entity-refresh-example[]
		doInJPA( this::entityManagerFactory, entityManager -> {
			AccountSummary summary = entityManager.find( AccountSummary.class, 1L );
			assertEquals( "John Doe", summary.getClientName() );
			assertEquals( 100 * 7000, summary.getBalance() );

			AccountTransaction transaction = new AccountTransaction();
			transaction.setAccount( entityManager.getReference( Account.class, 1L ) );
			transaction.setDescription( "Shopping" );
			transaction.setCents( -100 * 2200 );
			entityManager.persist( transaction );
			entityManager.flush();

			entityManager.refresh( summary );
			assertEquals( 100 * 4800, summary.getBalance() );
		} );

		//end::mapping-Subselect-entity-refresh-example[]
	}

	//tag::mapping-Subselect-example[]
	@Entity(name = "Client_subselect")
	@Table(name = "client_subselect")
	private static class Client {

		@Id
		private Long id;

		@Column(name = "first_name")
		private String firstName;

		@Column(name = "last_name")
		private String lastName;

		//Getters and setters omitted for brevity

		//end::mapping-Subselect-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		//tag::mapping-Subselect-example[]
	}

	@Entity(name = "Account_SubselectTest")
	private static class Account {

		@Id
		private Long id;

		@ManyToOne
		private Client client;

		private String description;

		//Getters and setters omitted for brevity

	//end::mapping-Subselect-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Client getClient() {
			return client;
		}

		public void setClient(Client client) {
			this.client = client;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
		//tag::mapping-Subselect-example[]
	}

	@Entity(name = "AccountTransaction_SubselectTest")
	private static class AccountTransaction {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private Account account;

		private Integer cents;

		private String description;

		//Getters and setters omitted for brevity

	//end::mapping-Subselect-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Account getAccount() {
			return account;
		}

		public void setAccount(Account account) {
			this.account = account;
		}

		public Integer getCents() {
			return cents;
		}

		public void setCents(Integer cents) {
			this.cents = cents;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		//tag::mapping-Subselect-example[]
	}

	@Entity(name = "AccountSummary_subselect")
	@Subselect(
		"select " +
		"	a.id as id, " +
		"	concat(concat(c.first_name, ' '), c.last_name) as clientName, " +
		"	sum(at_.cents) as balance " +
		"from Account_SubselectTest a " +
		"join client c on c.id = a.client_id " +
		"join AccountTransaction_SubselectTest at_ on a.id = at_.account_id " +
		"group by a.id, concat(concat(c.first_name, ' '), c.last_name)"
	)
	@Synchronize( {"client", "account", "account_transaction"} )
	private static class AccountSummary {

		@Id
		private Long id;

		private String clientName;

		private int balance;

		//Getters and setters omitted for brevity

	//end::mapping-Subselect-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getClientName() {
			return clientName;
		}

		public void setClientName(String clientName) {
			this.clientName = clientName;
		}

		public int getBalance() {
			return balance;
		}

		public void setBalance(int balance) {
			this.balance = balance;
		}
	//tag::mapping-Subselect-example[]
	}
	//end::mapping-Subselect-example[]
}
