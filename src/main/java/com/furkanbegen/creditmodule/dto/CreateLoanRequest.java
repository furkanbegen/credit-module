package com.furkanbegen.creditmodule.dto;

import com.furkanbegen.creditmodule.model.InstallmentOption;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CreateLoanRequest {

  @NotNull(message = "Loan amount is required")
  @Min(value = 1, message = "Loan amount must be greater than 0")
  private BigDecimal loanAmount;

  @NotNull(message = "Number of installments is required")
  private InstallmentOption numberOfInstallment;

  @NotNull(message = "Interest rate is required")
  @DecimalMin(value = "0.1", message = "Interest rate must be at least 0.1")
  @DecimalMax(value = "0.5", message = "Interest rate must be at most 0.5")
  private BigDecimal interestRate;
}
