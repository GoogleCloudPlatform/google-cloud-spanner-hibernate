package org.hibernate.dialect.entities;

import com.google.cloud.spanner.hibernate.types.SpannerJsonType;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

/**
 * A Test entity with JSON entity fields.
 */
@TypeDefs({
    @TypeDef(
        name = "json",
        typeClass = SpannerJsonType.class
    )
})
@Entity
public class JsonEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Type(type = "uuid-char")
  private UUID id;

  @Type(type = "json")
  private Employee employee;

  public UUID getId() {
    return id;
  }

  public Employee getEmployee() {
    return employee;
  }

  public void setEmployee(Employee employee) {
    this.employee = employee;
  }

  public static class Employee {
    public String name;
    public Address address;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Address getAddress() {
      return address;
    }

    public void setAddress(Address address) {
      this.address = address;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Employee)) {
        return false;
      }
      Employee employee = (Employee) o;
      return Objects.equals(name, employee.name) && Objects
          .equals(address, employee.address);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, address);
    }
  }

  public static class Address {
    public String address;
    public int zipCode;

    public Address(String address, int zipCode) {
      this.address = address;
      this.zipCode = zipCode;
    }

    public String getAddress() {
      return address;
    }

    public void setAddress(String address) {
      this.address = address;
    }

    public int getZipCode() {
      return zipCode;
    }

    public void setZipCode(int zipCode) {
      this.zipCode = zipCode;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Address)) {
        return false;
      }
      Address address1 = (Address) o;
      return zipCode == address1.zipCode && Objects.equals(address, address1.address);
    }

    @Override
    public int hashCode() {
      return Objects.hash(address, zipCode);
    }
  }
}
