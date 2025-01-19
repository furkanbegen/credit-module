package com.furkanbegen.creditmodule.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = "loan")
@Entity
@Table(name = "loan_installments")
public class LoanInstallment extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(name = "paid_amount")
  private BigDecimal paidAmount;

  @Column(name = "due_date", nullable = false)
  private LocalDateTime dueDate;

  @Column(name = "payment_date")
  private LocalDateTime paymentDate;

  @Column(name = "is_paid", nullable = false)
  private Boolean isPaid;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "loan_id")
  private Loan loan;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof LoanInstallment)) return false;
    LoanInstallment that = (LoanInstallment) o;
    return getId() != null && getId().equals(that.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
