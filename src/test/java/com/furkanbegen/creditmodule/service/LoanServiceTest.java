package com.furkanbegen.creditmodule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.furkanbegen.creditmodule.dto.CreateLoanRequest;
import com.furkanbegen.creditmodule.exception.InsufficientCreditLimitException;
import com.furkanbegen.creditmodule.model.Customer;
import com.furkanbegen.creditmodule.model.InstallmentOption;
import com.furkanbegen.creditmodule.model.Loan;
import com.furkanbegen.creditmodule.model.LoanInstallment;
import com.furkanbegen.creditmodule.repository.CustomerRepository;
import com.furkanbegen.creditmodule.repository.LoanRepository;
import com.furkanbegen.creditmodule.service.impl.LoanService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

  @Mock private CustomerRepository customerRepository;

  @Mock private LoanRepository loanRepository;

  @Captor private ArgumentCaptor<Loan> loanCaptor;

  @Captor private ArgumentCaptor<Customer> customerCaptor;

  private LoanService loanService;

  @BeforeEach
  void setUp() {
    loanService = new LoanService(customerRepository, loanRepository);
  }

  @Test
  void createLoan_WhenValidRequest_ShouldCreateLoanWithInstallments() {
    // Given
    Long customerId = 1L;
    CreateLoanRequest request = new CreateLoanRequest();
    BigDecimal loanAmount = BigDecimal.valueOf(12000);
    request.setLoanAmount(loanAmount);
    request.setNumberOfInstallment(InstallmentOption.TWELVE);
    request.setInterestRate(BigDecimal.valueOf(0.2));

    Customer customer = new Customer();
    customer.setId(customerId);
    customer.setCreditLimit(BigDecimal.valueOf(20000));
    customer.setUsedCreditLimit(BigDecimal.ZERO);

    when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
    when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // When
    loanService.createLoan(customerId, request);

    // Then
    verify(loanRepository).save(loanCaptor.capture());
    verify(customerRepository).save(customerCaptor.capture());

    Loan capturedLoan = loanCaptor.getValue();
    Customer capturedCustomer = customerCaptor.getValue();

    // Then - Verify loan details
    BigDecimal expectedLoanAmount =
        loanAmount
            .multiply(BigDecimal.ONE.add(request.getInterestRate()))
            .setScale(2, RoundingMode.HALF_UP);
    assertThat(capturedLoan.getLoanAmount()).isEqualTo(expectedLoanAmount);
    assertThat(capturedLoan.getNumberOfInstallment())
        .isEqualTo(request.getNumberOfInstallment().getValue());
    assertThat(capturedLoan.getIsPaid()).isFalse();

    // Then - Verify installments
    Set<LoanInstallment> installments = capturedLoan.getInstallments();
    assertThat(installments).hasSize(12);

    BigDecimal expectedInstallmentAmount =
        expectedLoanAmount.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

    installments.forEach(
        installment -> {
          assertThat(installment.getAmount()).isEqualTo(expectedInstallmentAmount);
          assertThat(installment.getPaidAmount()).isEqualTo(BigDecimal.ZERO);
          assertThat(installment.getIsPaid()).isFalse();
          assertThat(installment.getDueDate().getDayOfMonth()).isEqualTo(1);
        });

    // Then - Verify customer's used credit limit
    assertThat(capturedCustomer.getUsedCreditLimit()).isEqualTo(expectedLoanAmount);
  }

  @Test
  void createLoan_WhenCustomerNotFound_ShouldThrowEntityNotFoundException() {
    // Given
    Long customerId = 1L;
    CreateLoanRequest request = new CreateLoanRequest();
    when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

    // When/Then
    assertThrows(EntityNotFoundException.class, () -> loanService.createLoan(customerId, request));

    // Then - Verify no interactions
    verify(loanRepository, never()).save(any());
    verify(customerRepository, never()).save(any());
  }

  @Test
  void createLoan_WhenInsufficientCreditLimit_ShouldThrowInsufficientCreditLimitException() {
    // Given
    Long customerId = 1L;
    CreateLoanRequest request = new CreateLoanRequest();
    request.setLoanAmount(BigDecimal.valueOf(12000));
    request.setNumberOfInstallment(InstallmentOption.TWELVE);
    request.setInterestRate(BigDecimal.valueOf(0.2));

    Customer customer = new Customer();
    customer.setId(customerId);
    customer.setCreditLimit(BigDecimal.valueOf(10000));
    customer.setUsedCreditLimit(BigDecimal.ZERO);

    when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

    // When/Then
    assertThrows(
        InsufficientCreditLimitException.class, () -> loanService.createLoan(customerId, request));

    // Then - Verify no interactions
    verify(loanRepository, never()).save(any());
    verify(customerRepository, never()).save(any());
  }

  @Test
  void createLoan_WhenPartiallyUsedCreditLimit_ShouldConsiderRemainingLimit() {
    // Given
    Long customerId = 1L;
    CreateLoanRequest request = new CreateLoanRequest();
    BigDecimal loanAmount = BigDecimal.valueOf(5000);
    request.setLoanAmount(loanAmount);
    request.setNumberOfInstallment(InstallmentOption.SIX);
    request.setInterestRate(BigDecimal.valueOf(0.2));

    Customer customer = new Customer();
    customer.setId(customerId);
    customer.setCreditLimit(BigDecimal.valueOf(10000));
    int usedCreditLimit = 3000;
    customer.setUsedCreditLimit(BigDecimal.valueOf(usedCreditLimit));

    when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
    when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // When
    loanService.createLoan(customerId, request);

    // Then
    verify(customerRepository).save(customerCaptor.capture());
    Customer capturedCustomer = customerCaptor.getValue();

    BigDecimal expectedLoanAmount =
        loanAmount
            .multiply(BigDecimal.ONE.add(request.getInterestRate()))
            .setScale(2, RoundingMode.HALF_UP);
    BigDecimal expectedNewUsedLimit = BigDecimal.valueOf(usedCreditLimit).add(expectedLoanAmount);
    assertThat(capturedCustomer.getUsedCreditLimit()).isEqualTo(expectedNewUsedLimit);
  }

  @Test
  void createLoan_ShouldCalculateCorrectTotalAmountAndEqualInstallments() {
    // Given
    Long customerId = 1L;
    CreateLoanRequest request = new CreateLoanRequest();
    request.setLoanAmount(BigDecimal.valueOf(10000).setScale(2, RoundingMode.HALF_UP));
    request.setNumberOfInstallment(InstallmentOption.TWELVE);
    request.setInterestRate(BigDecimal.valueOf(0.2).setScale(2, RoundingMode.HALF_UP));

    Customer customer = new Customer();
    customer.setId(customerId);
    customer.setCreditLimit(BigDecimal.valueOf(20000).setScale(2, RoundingMode.HALF_UP));
    customer.setUsedCreditLimit(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

    when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
    when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // When
    loanService.createLoan(customerId, request);

    // Then
    verify(loanRepository).save(loanCaptor.capture());
    Loan capturedLoan = loanCaptor.getValue();
    Set<LoanInstallment> installments = capturedLoan.getInstallments();

    // Verify total amount calculation: amount * (1 + interest rate)
    BigDecimal expectedTotalAmount =
        request
            .getLoanAmount()
            .multiply(BigDecimal.ONE.add(request.getInterestRate()))
            .setScale(2, RoundingMode.HALF_UP);

    // Verify each installment has same amount
    BigDecimal expectedInstallmentAmount =
        expectedTotalAmount.divide(
            BigDecimal.valueOf(request.getNumberOfInstallment().getValue()),
            2,
            RoundingMode.HALF_UP);

    // Sum all installment amounts to verify they equal total amount
    BigDecimal actualTotalAmount =
        installments.stream()
            .map(LoanInstallment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

    // Verify all installments have same amount
    assertThat(installments)
        .extracting(LoanInstallment::getAmount)
        .containsOnly(expectedInstallmentAmount);

    // Verify total amount matches expected
    assertThat(actualTotalAmount.compareTo(expectedTotalAmount)).isZero();
  }
}
