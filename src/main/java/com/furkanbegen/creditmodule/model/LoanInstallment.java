package com.furkanbegen.creditmodule.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "loan_installments")
@EqualsAndHashCode(callSuper = true)
public class LoanInstallment extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "loan_id", nullable = false)
  private Loan loan;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(name = "paid_amount", nullable = false)
  private BigDecimal paidAmount;

  @Column(name = "due_date", nullable = false)
  private LocalDateTime dueDate;

  @Column(name = "payment_date")
  private LocalDateTime paymentDate;

  @Column(name = "is_paid", nullable = false)
  private Boolean isPaid;
}
