package org.hibernate.dialect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.persistence.PersistenceException;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.entities.foreignkey.Cart;
import org.hibernate.dialect.entities.foreignkey.Item;
import org.hibernate.dialect.entities.foreignkey.CartSession;
import org.hibernate.jpa.test.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class ForeignKeyTest extends BaseCoreFunctionalTestCase {

  private static final String SESSION_CART_FOREIGN_KEY_CONSTRAINT_NAME = "Fk_sessionDetails_cartId";
  private static final String ITEM_CART_FOREIGN_KEY_CONSTRAINT_NAME = "Fk_itemDetails_cartId";
  private static final String DELETE_RULE_COLUMN_NAME = "DELETE_RULE";
  private static final String CONSTRAINT_NAME_COLUMN_NAME = "CONSTRAINT_NAME";
  private static final String DELETE_RULE_CASCADE_VALUE = "CASCADE";
  private static final String DELETE_RULE_DEFAULT_VALUE = "NO ACTION";

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class<?>[]{
        Cart.class,
        Item.class,
        CartSession.class
    };
  }

  @Test
  public void testForeignKeyConstraint_verifyDDL() throws Exception {

      doInHibernate(this::sessionFactory, session -> {
        Cart cart = new Cart();
        cart.setCartId(1L);
        cart.setItems(new HashSet<>());

        Item item = new Item();
        item.setItemId(1L);
        item.setCart(cart);

        CartSession cartSession = new CartSession();
        cartSession.setSessionId(1L);
        cartSession.setCart(cart);

        cart.addItem(item);
        cart.setSession(cartSession);

        session.persist(cart);
        session.persist(item);
        session.persist(cartSession);
      });

    verifyOnDeleteCascadeDDL();
  }

  @Test
  public void testForeignKeyConstraint_verifyValidInserts() {

    doInHibernate(this::sessionFactory, session -> {
      Cart cart = new Cart();
      cart.setCartId(2L);
      cart.setItems(new HashSet<>());

      Item item = new Item();
      item.setItemId(2L);
      item.setCart(cart);

      CartSession cartSession = new CartSession();
      cartSession.setSessionId(2L);
      cartSession.setCart(cart);

      cart.addItem(item);
      cart.setSession(cartSession);

      session.persist(cart);
      session.persist(item);
      session.persist(cartSession);
    });

    // validate that records exist
    doInHibernate( this::sessionFactory, session -> {
      Item item = session.get( Item.class, 2L );
      assertEquals(2L, item.getCart().cartId);
      CartSession cartSession = session.get( CartSession.class, 2L );
      assertEquals(2L, cartSession.getCart().cartId);
    } );
  }

  @Test
  public void testForeignKeyConstraint_verifyInvalidInserts() {
    try {
      doInHibernate(this::sessionFactory, session -> {
        Cart cart = new Cart();
        cart.setCartId(3L);
        cart.setItems(new HashSet<>());

        Item item = new Item();
        item.setItemId(3L);
        item.setCart(cart);

        CartSession cartSession = new CartSession();
        cartSession.setSessionId(3L);
        cartSession.setCart(cart);

        cart.addItem(item);
        cart.setSession(cartSession);

        session.persist(item);
      });
      fail("Expected exception");
    } catch (final PersistenceException ex) {
      assertThat(ex.getCause().getCause().getMessage()).contains("Foreign key constraint `Fk_itemDetails_cartId` is "
          + "violated on table `Item`");
    }
  }

  @Test
  public void testForeignKeyConstraint_verifyDeletingReferencedKey() throws Exception {

    doInHibernate(this::sessionFactory, session -> {
      Cart cart = new Cart();
      cart.setCartId(4L);
      cart.setItems(new HashSet<>());

      Item item = new Item();
      item.setItemId(4L);
      item.setCart(cart);

      CartSession cartSession = new CartSession();
      cartSession.setSessionId(4L);
      cartSession.setCart(cart);

      cart.addItem(item);
      cart.setSession(cartSession);

      session.persist(cart);
      session.persist(item);
      session.persist(cartSession);

      // Delete the referenced cart object referenced by Item/CartSession child tables.
      session.delete(cart);

    });

    doInHibernate(this::sessionFactory, session -> {
      // Verify cascade deletes in child tables ensuring that the referencing records get deleted.
      assertThat(session.get(Item.class, 4L)).isNull();
      assertThat(session.get(CartSession.class, 4L)).isNull();
    });


    verifyOnDeleteCascadeDDL();
  }

  /**
   * This method uses a JDBC connection to query for the tables that exist in the Spanner database
   * and verifies the foreign relationships are created between tables.
   */
  private static void verifyOnDeleteCascadeDDL() throws SQLException {
    Configuration configuration = new Configuration();

    try (Connection jdbcConnection =
        DriverManager.getConnection(configuration.getProperty("hibernate.connection.url"))) {
      final Statement statement = jdbcConnection.createStatement();
      statement.execute("SELECT CONSTRAINT_NAME, DELETE_RULE\n"
          + "FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS\n");

      final Map<String, String> actualReferentialQueryDetails = new HashMap<>();
      final ResultSet tablesResultSet = statement.getResultSet();
      while (tablesResultSet.next()) {
        actualReferentialQueryDetails.put(tablesResultSet.getString(CONSTRAINT_NAME_COLUMN_NAME), tablesResultSet.getString(DELETE_RULE_COLUMN_NAME));
      }
      assertThat(actualReferentialQueryDetails).contains(new AbstractMap.SimpleEntry(ITEM_CART_FOREIGN_KEY_CONSTRAINT_NAME, DELETE_RULE_CASCADE_VALUE));
      assertThat(actualReferentialQueryDetails).contains(new AbstractMap.SimpleEntry(SESSION_CART_FOREIGN_KEY_CONSTRAINT_NAME, DELETE_RULE_DEFAULT_VALUE));
    }

  }
}
