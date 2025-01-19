package com.furkanbegen.creditmodule.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoanPaymentResponse {
  private int numberOfInstallmentsPaid;
  private BigDecimal totalAmountPaid;
  private boolean isLoanFullyPaid;
  private BigDecimal totalDiscount;
  private BigDecimal totalPenalty;
}
