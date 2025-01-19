package com.furkanbegen.creditmodule.dto;

import com.furkanbegen.creditmodule.model.InstallmentOption;
import lombok.Data;

@Data
public class LoanFilterDTO {
  private Boolean isPaid;
  private InstallmentOption numberOfInstallment;
  private Boolean isOverdue;
}
