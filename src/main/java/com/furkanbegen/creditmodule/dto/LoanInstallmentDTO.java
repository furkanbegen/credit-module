package com.furkanbegen.creditmodule.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class LoanInstallmentDTO {
  private Long id;
  private BigDecimal amount;
  private BigDecimal paidAmount;
  private LocalDateTime dueDate;
  private LocalDateTime paymentDate;
  private Boolean isPaid;
}
