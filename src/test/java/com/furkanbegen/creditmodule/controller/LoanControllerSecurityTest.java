package com.furkanbegen.creditmodule.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.furkanbegen.creditmodule.config.SecurityTestConfig;
import com.furkanbegen.creditmodule.dto.CreateLoanRequest;
import com.furkanbegen.creditmodule.dto.LoanResponseDTO;
import com.furkanbegen.creditmodule.mapper.LoanMapper;
import com.furkanbegen.creditmodule.model.Customer;
import com.furkanbegen.creditmodule.model.InstallmentOption;
import com.furkanbegen.creditmodule.model.Loan;
import com.furkanbegen.creditmodule.repository.CustomerRepository;
import com.furkanbegen.creditmodule.repository.UserRepository;
import com.furkanbegen.creditmodule.service.impl.LoanService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LoanController.class)
@Import(SecurityTestConfig.class)
class LoanControllerSecurityTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private LoanService loanService;

  @Autowired private CustomerRepository customerRepository;

  @MockitoBean private LoanMapper loanMapper;

  @Autowired private UserRepository userRepository;

  private static final String BASE_URL = "/api/v1/customers";

  @BeforeEach
  void setUp() {
    reset(customerRepository);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void whenAdminAccess_thenSuccess() throws Exception {
    // Given
    Long customerId = 1L;
    CreateLoanRequest request = new CreateLoanRequest();
    request.setLoanAmount(BigDecimal.valueOf(10000));
    request.setInterestRate(BigDecimal.valueOf(0.1));
    request.setNumberOfInstallment(InstallmentOption.TWELVE);

    when(loanService.createLoan(eq(customerId), any(CreateLoanRequest.class)))
        .thenReturn(new Loan());
    when(loanMapper.toDTO(any(Loan.class))).thenReturn(new LoanResponseDTO());

    // When/Then
    mockMvc
        .perform(
            post(BASE_URL + "/{customerId}/loans", customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "customer@test.com", roles = "CUSTOMER")
  void whenCustomerAccessOwnData_thenSuccess() throws Exception {
    // Given
    Long customerId = 1L;

    Customer customer = new Customer();
    customer.setId(customerId);

    when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer));
    when(loanService.getLoans(eq(customerId), any())).thenReturn(Collections.emptyList());

    // When/Then
    mockMvc
        .perform(
            get(BASE_URL + "/{customerId}/loans", customerId)
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk());

    verify(customerRepository).findByUserId(1L);
  }

  @Test
  @WithMockUser(username = "customer@test.com", roles = "CUSTOMER")
  void whenCustomerAccessOtherCustomerData_thenForbidden() throws Exception {
    // Given
    Long customerId = 2L;

    Customer customer = new Customer();
    customer.setId(1L);

    when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer));

    // When/Then
    mockMvc
        .perform(
            get(BASE_URL + "/{customerId}/loans", customerId)
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void whenUnauthenticated_thenUnauthorized() throws Exception {
    // Given
    Long customerId = 1L;

    // When/Then
    mockMvc
        .perform(
            get(BASE_URL + "/{customerId}/loans", customerId)
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isUnauthorized());
  }
}
