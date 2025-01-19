package com.furkanbegen.creditmodule.mapper;

import com.furkanbegen.creditmodule.dto.LoanResponseDTO;
import com.furkanbegen.creditmodule.model.Loan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoanMapper {

  private final InstallmentMapper installmentMapper;

  public LoanResponseDTO toDTO(Loan loan) {
    if (loan == null) {
      return null;
    }

    var dto = new LoanResponseDTO();
    dto.setId(loan.getId());
    dto.setCustomerId(loan.getCustomer().getId());
    dto.setLoanAmount(loan.getLoanAmount());
    dto.setNumberOfInstallment(loan.getNumberOfInstallment());
    dto.setCreateDate(loan.getCreateDate());
    dto.setInstallments(installmentMapper.toDTOSet(loan.getInstallments()));

    return dto;
  }
}
