package org.hibernate.dialect.entities.foreignkey;

import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

@Entity
public class Cart {

  @Id
  public long cartId;

  @OneToMany(mappedBy="cart")
  public Set<Item> items;

  @OneToOne(mappedBy="cart", cascade= CascadeType.REMOVE)
  public CartSession session;

  public Cart() {
  }

  public long getCartId() {
    return cartId;
  }

  public void setCartId(long cartId) {
    this.cartId = cartId;
  }

  public Set<Item> getItems() {
    return items;
  }

  public void setItems(Set<Item> items) {
    this.items = items;
  }

  public void addItem(Item item) {
    this.items.add(item);
  }

  public CartSession getSession() {
    return session;
  }

  public void setSession(CartSession session) {
    this.session = session;
  }
}
