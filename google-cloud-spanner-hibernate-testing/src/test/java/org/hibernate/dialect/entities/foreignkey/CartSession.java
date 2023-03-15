package org.hibernate.dialect.entities.foreignkey;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import org.hibernate.annotations.ForeignKey;

@Entity
public class CartSession {

  @Id
  public long sessionId;

  @OneToOne
  @JoinColumn(name="cartId")
  @ForeignKey(name="Fk_sessionDetails_cartId")
  private Cart cart;

  public CartSession() {
  }

  public long getSessionId() {
    return sessionId;
  }

  public void setSessionId(long sessionId) {
    this.sessionId = sessionId;
  }

  public Cart getCart() {
    return cart;
  }

  public void setCart(Cart cart) {
    this.cart = cart;
  }
}