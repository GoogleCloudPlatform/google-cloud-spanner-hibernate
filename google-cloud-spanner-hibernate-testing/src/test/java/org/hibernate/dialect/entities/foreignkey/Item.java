package org.hibernate.dialect.entities.foreignkey;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
public class Item {

  @Id
  public long itemId;

  @ManyToOne
  @JoinColumn(name="cartId")
  @ForeignKey(name="Fk_itemDetails_cartId")
  @OnDelete(action = OnDeleteAction.CASCADE)
  public Cart cart;

  public Item() {
  }

  public long getItemId() {
    return itemId;
  }

  public void setItemId(long itemId) {
    this.itemId = itemId;
  }

  public Cart getCart() {
    return cart;
  }

  public void setCart(Cart cart) {
    this.cart = cart;
  }
}