package com.furkanbegen.creditmodule.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = {"customer", "installments"})
@Entity
@Table(name = "loans")
public class Loan extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "loan_amount", nullable = false)
  private BigDecimal loanAmount;

  @Column(name = "interest_rate", nullable = false)
  private BigDecimal interestRate;

  @Column(name = "number_of_installment", nullable = false)
  private Integer numberOfInstallment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id")
  private Customer customer;

  @Column(name = "create_date", nullable = false)
  private LocalDateTime createDate;

  @Column(name = "is_paid", nullable = false)
  private Boolean isPaid;

  @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL)
  private Set<LoanInstallment> installments;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Loan)) return false;
    Loan loan = (Loan) o;
    return getId() != null && getId().equals(loan.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
