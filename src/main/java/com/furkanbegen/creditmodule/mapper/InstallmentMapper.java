package com.furkanbegen.creditmodule.mapper;

import com.furkanbegen.creditmodule.dto.LoanInstallmentDTO;
import com.furkanbegen.creditmodule.model.LoanInstallment;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class InstallmentMapper {

  public LoanInstallmentDTO toDTO(LoanInstallment installment) {
    if (installment == null) {
      return null;
    }

    var dto = new LoanInstallmentDTO();
    dto.setId(installment.getId());
    dto.setAmount(installment.getAmount());
    dto.setPaidAmount(installment.getPaidAmount());
    dto.setDueDate(installment.getDueDate());
    dto.setPaymentDate(installment.getPaymentDate());
    dto.setIsPaid(installment.getIsPaid());

    return dto;
  }

  public Set<LoanInstallmentDTO> toDTOSet(Set<LoanInstallment> installments) {
    if (installments == null) {
      return null;
    }

    return installments.stream().map(this::toDTO).collect(Collectors.toSet());
  }
}
