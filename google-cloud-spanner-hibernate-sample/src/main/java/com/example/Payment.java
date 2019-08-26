package com.example;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import org.hibernate.annotations.Type;

// [START spanner_hibernate_inheritance]
/**
 * An example {@link Entity} which demonstrates usage of {@link Inheritance}.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Type(type = "uuid-char")
  private UUID id;

  private Long amount;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Long getAmount() {
    return amount;
  }

  public void setAmount(Long amount) {
    this.amount = amount;
  }
}

@Entity
class WireTransferPayment extends Payment {
  private String wireId;

  public String getWireId() {
    return wireId;
  }

  public void setWireId(String wireId) {
    this.wireId = wireId;
  }
}

@Entity
class CreditCardPayment extends Payment {
  private String creditCardId;

  public String getCreditCardId() {
    return creditCardId;
  }

  public void setCreditCardId(String creditCardId) {
    this.creditCardId = creditCardId;
  }
}
// [END spanner_hibernate_inheritance]
