package com.furkanbegen.creditmodule.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = {"loans", "user"})
@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String surname;

  @Column(name = "credit_limit", nullable = false)
  private BigDecimal creditLimit;

  @Column(name = "used_credit_limit", nullable = false)
  private BigDecimal usedCreditLimit;

  @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
  private Set<Loan> loans;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Customer)) return false;
    Customer customer = (Customer) o;
    return getId() != null && getId().equals(customer.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
