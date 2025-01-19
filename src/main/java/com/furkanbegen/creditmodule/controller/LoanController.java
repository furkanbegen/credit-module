package com.furkanbegen.creditmodule.controller;

import static com.furkanbegen.creditmodule.constant.AppConstant.API_BASE_PATH;

import com.furkanbegen.creditmodule.dto.CreateLoanRequest;
import com.furkanbegen.creditmodule.dto.LoanResponseDTO;
import com.furkanbegen.creditmodule.mapper.LoanMapper;
import com.furkanbegen.creditmodule.service.impl.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(API_BASE_PATH + "/customers/{customerId}/loans")
@RequiredArgsConstructor
public class LoanController {

  private final LoanService loanService;
  private final LoanMapper loanMapper;

  @PostMapping
  public ResponseEntity<LoanResponseDTO> createLoan(
      @PathVariable Long customerId, @Valid @RequestBody CreateLoanRequest request) {
    return ResponseEntity.ok(loanMapper.toDTO(loanService.createLoan(customerId, request)));
  }
}
