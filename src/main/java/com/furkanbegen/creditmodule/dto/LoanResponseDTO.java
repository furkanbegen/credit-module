package com.furkanbegen.creditmodule.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Data;

@Data
public class LoanResponseDTO {
  private Long id;
  private Long customerId;
  private BigDecimal loanAmount;
  private BigDecimal totalAmount;
  private Integer numberOfInstallment;
  private LocalDateTime createDate;
  private Set<LoanInstallmentDTO> installments;
}
