package com.furkanbegen.creditmodule.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "loans")
@EqualsAndHashCode(callSuper = true)
public class Loan extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Column(name = "loan_amount", nullable = false)
  private BigDecimal loanAmount;

  @Column(name = "number_of_installment", nullable = false)
  private Integer numberOfInstallment;

  @Column(name = "create_date", nullable = false)
  private LocalDateTime createDate;

  @Column(name = "is_paid", nullable = false)
  private Boolean isPaid;

  @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL)
  private Set<LoanInstallment> installments;
}
