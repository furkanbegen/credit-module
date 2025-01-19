package com.furkanbegen.creditmodule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.furkanbegen.creditmodule.dto.CreateLoanRequest;
import com.furkanbegen.creditmodule.dto.LoanFilterDTO;
import com.furkanbegen.creditmodule.dto.LoanPaymentRequest;
import com.furkanbegen.creditmodule.dto.LoanPaymentResponse;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
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

  @Test
  void getLoans_WhenCustomerNotFound_ShouldThrowEntityNotFoundException() {
    // Given
    Long customerId = 999L;
    when(customerRepository.existsById(customerId)).thenReturn(false);

    // When/Then
    assertThrows(
        EntityNotFoundException.class, () -> loanService.getLoans(customerId, new LoanFilterDTO()));
  }

  @Test
  void getLoans_WhenNoFilters_ShouldReturnAllLoans() {
    // Given
    Long customerId = 1L;
    when(customerRepository.existsById(customerId)).thenReturn(true);

    List<Loan> expectedLoans = createSampleLoans();
    when(loanRepository.findLoansWithFilters(
            eq(customerId), isNull(), isNull(), isNull(), any(LocalDateTime.class)))
        .thenReturn(expectedLoans);

    // When
    List<Loan> result = loanService.getLoans(customerId, null);

    // Then
    assertThat(result).hasSize(3);
    verify(loanRepository)
        .findLoansWithFilters(
            eq(customerId), isNull(), isNull(), isNull(), any(LocalDateTime.class));
  }

  @Test
  void getLoans_WhenFilterByPaidStatus_ShouldReturnOnlyPaidLoans() {
    // Given
    Long customerId = 1L;
    when(customerRepository.existsById(customerId)).thenReturn(true);

    LoanFilterDTO filter = new LoanFilterDTO();
    filter.setIsPaid(true);

    List<Loan> expectedLoans = createSampleLoans().stream().filter(Loan::getIsPaid).toList();

    when(loanRepository.findLoansWithFilters(
            eq(customerId), eq(true), isNull(), isNull(), any(LocalDateTime.class)))
        .thenReturn(expectedLoans);

    // When
    List<Loan> result = loanService.getLoans(customerId, filter);

    // Then
    assertThat(result).isNotEmpty().allMatch(Loan::getIsPaid);
  }

  @Test
  void getLoans_WhenFilterByInstallments_ShouldReturnLoansWithMatchingInstallments() {
    // Given
    Long customerId = 1L;
    when(customerRepository.existsById(customerId)).thenReturn(true);

    LoanFilterDTO filter = new LoanFilterDTO();
    filter.setNumberOfInstallment(InstallmentOption.TWELVE);

    List<Loan> expectedLoans =
        createSampleLoans().stream().filter(loan -> loan.getNumberOfInstallment() == 12).toList();

    when(loanRepository.findLoansWithFilters(
            eq(customerId), isNull(), eq(12), isNull(), any(LocalDateTime.class)))
        .thenReturn(expectedLoans);

    // When
    List<Loan> result = loanService.getLoans(customerId, filter);

    // Then
    assertThat(result).isNotEmpty().allMatch(loan -> loan.getNumberOfInstallment() == 12);
  }

  @Test
  void getLoans_WhenFilterByOverdue_ShouldReturnOverdueLoans() {
    // Given
    Long customerId = 1L;
    when(customerRepository.existsById(customerId)).thenReturn(true);

    LoanFilterDTO filter = new LoanFilterDTO();
    filter.setIsOverdue(true);

    List<Loan> expectedLoans =
        createSampleLoans().stream().filter(this::hasOverdueInstallments).toList();

    when(loanRepository.findLoansWithFilters(
            eq(customerId), isNull(), isNull(), eq(true), any(LocalDateTime.class)))
        .thenReturn(expectedLoans);

    // When
    List<Loan> result = loanService.getLoans(customerId, filter);

    // Then
    assertThat(result).isNotEmpty().allMatch(this::hasOverdueInstallments);
  }

  @Test
  void getLoans_WhenMultipleFilters_ShouldApplyAllFilters() {
    // Given
    Long customerId = 1L;
    when(customerRepository.existsById(customerId)).thenReturn(true);

    LoanFilterDTO filter = new LoanFilterDTO();
    filter.setIsPaid(false);
    filter.setNumberOfInstallment(InstallmentOption.TWELVE);
    filter.setIsOverdue(true);

    List<Loan> expectedLoans =
        createSampleLoans().stream()
            .filter(
                loan ->
                    !loan.getIsPaid()
                        && loan.getNumberOfInstallment() == 12
                        && hasOverdueInstallments(loan))
            .toList();

    when(loanRepository.findLoansWithFilters(
            eq(customerId), eq(false), eq(12), eq(true), any(LocalDateTime.class)))
        .thenReturn(expectedLoans);

    // When
    List<Loan> result = loanService.getLoans(customerId, filter);

    // Then
    assertThat(result)
        .isNotEmpty()
        .allMatch(
            loan ->
                !loan.getIsPaid()
                    && loan.getNumberOfInstallment() == 12
                    && hasOverdueInstallments(loan));
  }

  @Test
  void getLoanWithInstallments_WhenLoanExists_ShouldReturnLoanWithInstallments() {
    // Given
    Long customerId = 1L;
    Long loanId = 1L;

    Loan expectedLoan = createLoan(loanId, false, 12, false);
    when(loanRepository.findByIdAndCustomerId(loanId, customerId))
        .thenReturn(Optional.of(expectedLoan));

    // When
    Loan result = loanService.getLoanWithInstallments(customerId, loanId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(loanId);
    assertThat(result.getInstallments()).hasSize(12);
    verify(loanRepository).findByIdAndCustomerId(loanId, customerId);
  }

  @Test
  void getLoanWithInstallments_WhenLoanDoesNotExist_ShouldThrowEntityNotFoundException() {
    // Given
    Long customerId = 1L;
    Long loanId = 999L;

    when(loanRepository.findByIdAndCustomerId(loanId, customerId)).thenReturn(Optional.empty());

    // When/Then
    EntityNotFoundException exception =
        assertThrows(
            EntityNotFoundException.class,
            () -> loanService.getLoanWithInstallments(customerId, loanId));

    // Then
    assertThat(exception.getMessage())
        .isEqualTo(
            String.format("Loan not found with id: %d for customer: %d", loanId, customerId));
    verify(loanRepository).findByIdAndCustomerId(loanId, customerId);
  }

  @Test
  void getLoanWithInstallments_ShouldReturnInstallmentsInOrder() {
    // Given
    Long customerId = 1L;
    Long loanId = 1L;

    Loan loan = createLoanWithOrderedInstallments(loanId, 12);
    when(loanRepository.findByIdAndCustomerId(loanId, customerId)).thenReturn(Optional.of(loan));

    // When
    Loan result = loanService.getLoanWithInstallments(customerId, loanId);

    // Then
    List<LoanInstallment> installments = new ArrayList<>(result.getInstallments());
    assertThat(installments).isSortedAccordingTo(Comparator.comparing(LoanInstallment::getDueDate));
  }

  @Test
  void payLoan_WhenPayingMultipleInstallments_ShouldPayEarliestFirst() {
    // Given
    Long customerId = 1L;
    Long loanId = 1L;
    BigDecimal installmentAmount = BigDecimal.valueOf(1000);

    Loan loan = createLoanWithInstallments(loanId, installmentAmount, 3);
    when(loanRepository.findByIdAndCustomerId(loanId, customerId)).thenReturn(Optional.of(loan));

    LoanPaymentRequest request = new LoanPaymentRequest();
    request.setPaymentAmount(BigDecimal.valueOf(2000)); // Enough for 2 installments

    // When
    LoanPaymentResponse response = loanService.payLoan(customerId, loanId, request);

    // Then
    assertThat(response.getNumberOfInstallmentsPaid()).isEqualTo(2);
    assertThat(response.isLoanFullyPaid()).isFalse();

    List<LoanInstallment> installments = new ArrayList<>(loan.getInstallments());
    installments.sort(Comparator.comparing(LoanInstallment::getDueDate));

    // First two installments should be paid
    assertThat(installments.get(0).getIsPaid()).isTrue();
    assertThat(installments.get(1).getIsPaid()).isTrue();
    // Last installment should remain unpaid
    assertThat(installments.get(2).getIsPaid()).isFalse();
  }

  @Test
  void payLoan_WhenPaymentInsufficientForAnyInstallment_ShouldThrowException() {
    // Given
    Long customerId = 1L;
    Long loanId = 1L;
    BigDecimal installmentAmount = BigDecimal.valueOf(1000);

    Loan loan = createLoanWithInstallments(loanId, installmentAmount, 3);
    when(loanRepository.findByIdAndCustomerId(loanId, customerId)).thenReturn(Optional.of(loan));

    LoanPaymentRequest request = new LoanPaymentRequest();
    request.setPaymentAmount(BigDecimal.valueOf(500)); // Less than one installment

    // When/Then
    assertThrows(
        IllegalArgumentException.class, () -> loanService.payLoan(customerId, loanId, request));
  }

  @Test
  void payLoan_WhenPayingBeforeDueDate_ShouldApplyDiscount() {
    // Given
    Long customerId = 1L;
    Long loanId = 1L;
    BigDecimal installmentAmount = BigDecimal.valueOf(1000);

    // Create customer
    Customer customer = new Customer();
    customer.setId(customerId);
    customer.setUsedCreditLimit(installmentAmount);

    Loan loan = createLoanWithInstallments(loanId, installmentAmount, 1);
    loan.setCustomer(customer); // Set customer
    loan.setLoanAmount(installmentAmount);

    // Set due date to 10 days in future
    loan.getInstallments().iterator().next().setDueDate(LocalDateTime.now().plusDays(10));

    when(loanRepository.findByIdAndCustomerId(loanId, customerId)).thenReturn(Optional.of(loan));

    LoanPaymentRequest request = new LoanPaymentRequest();
    request.setPaymentAmount(BigDecimal.valueOf(1000));

    // When
    LoanPaymentResponse response = loanService.payLoan(customerId, loanId, request);

    // Then
    assertThat(response.getTotalDiscount()).isGreaterThan(BigDecimal.ZERO);
    assertThat(response.getTotalPenalty()).isEqualTo(BigDecimal.ZERO);
    assertThat(response.getTotalAmountPaid()).isLessThan(installmentAmount);
    verify(loanRepository).save(loan);
  }

  @Test
  void payLoan_WhenPayingAfterDueDate_ShouldApplyPenalty() {
    // Given
    Long customerId = 1L;
    Long loanId = 1L;
    BigDecimal installmentAmount = BigDecimal.valueOf(1000);

    // Create customer
    Customer customer = new Customer();
    customer.setId(customerId);
    customer.setUsedCreditLimit(installmentAmount);

    Loan loan = createLoanWithInstallments(loanId, installmentAmount, 1);
    loan.setCustomer(customer); // Set customer
    loan.setLoanAmount(installmentAmount);

    // Set due date to 10 days in past
    loan.getInstallments().iterator().next().setDueDate(LocalDateTime.now().minusDays(10));

    when(loanRepository.findByIdAndCustomerId(loanId, customerId)).thenReturn(Optional.of(loan));

    LoanPaymentRequest request = new LoanPaymentRequest();
    request.setPaymentAmount(BigDecimal.valueOf(1100)); // Include buffer for penalty

    // When
    LoanPaymentResponse response = loanService.payLoan(customerId, loanId, request);

    // Then
    assertThat(response.getTotalPenalty()).isGreaterThan(BigDecimal.ZERO);
    assertThat(response.getTotalDiscount()).isEqualTo(BigDecimal.ZERO);
    assertThat(response.getTotalAmountPaid()).isGreaterThan(installmentAmount);
    verify(loanRepository).save(loan);
  }

  @Test
  void payLoan_WhenPayingFullLoan_ShouldUpdateCustomerCreditLimit() {
    // Given
    Long customerId = 1L;
    Long loanId = 1L;
    BigDecimal installmentAmount = BigDecimal.valueOf(1000);
    BigDecimal totalLoanAmount = BigDecimal.valueOf(3000);

    Customer customer = new Customer();
    customer.setUsedCreditLimit(totalLoanAmount);

    Loan loan = createLoanWithInstallments(loanId, installmentAmount, 3);
    loan.setLoanAmount(totalLoanAmount);
    loan.setCustomer(customer);

    when(loanRepository.findByIdAndCustomerId(loanId, customerId)).thenReturn(Optional.of(loan));

    LoanPaymentRequest request = new LoanPaymentRequest();
    request.setPaymentAmount(BigDecimal.valueOf(3000));

    // When
    LoanPaymentResponse response = loanService.payLoan(customerId, loanId, request);

    // Then
    assertThat(response.isLoanFullyPaid()).isTrue();
    assertThat(loan.getIsPaid()).isTrue();
    assertThat(customer.getUsedCreditLimit()).isEqualTo(BigDecimal.ZERO);

    verify(customerRepository).save(customer);
  }

  @Test
  void payLoan_WhenLoanAlreadyPaid_ShouldThrowException() {
    // Given
    Long customerId = 1L;
    Long loanId = 1L;

    Loan loan = createLoanWithInstallments(loanId, BigDecimal.valueOf(1000), 1);
    loan.setIsPaid(true);

    when(loanRepository.findByIdAndCustomerId(loanId, customerId)).thenReturn(Optional.of(loan));

    LoanPaymentRequest request = new LoanPaymentRequest();
    request.setPaymentAmount(BigDecimal.valueOf(1000));

    // When/Then
    assertThrows(
        IllegalStateException.class, () -> loanService.payLoan(customerId, loanId, request));
  }

  @Test
  void payLoan_WhenInstallmentsTooFarInFuture_ShouldThrowException() {
    // Given
    Long customerId = 1L;
    Long loanId = 1L;
    BigDecimal installmentAmount = BigDecimal.valueOf(1000);

    Loan loan = createLoanWithInstallments(loanId, installmentAmount, 1);
    loan.getInstallments()
        .iterator()
        .next()
        .setDueDate(LocalDateTime.now().plusMonths(4)); // Beyond 3 months

    when(loanRepository.findByIdAndCustomerId(loanId, customerId)).thenReturn(Optional.of(loan));

    LoanPaymentRequest request = new LoanPaymentRequest();
    request.setPaymentAmount(BigDecimal.valueOf(1000));

    // When/Then
    assertThrows(
        IllegalStateException.class, () -> loanService.payLoan(customerId, loanId, request));
  }

  private List<Loan> createSampleLoans() {
    return List.of(
        createLoan(1L, true, 12, false),
        createLoan(2L, false, 6, true),
        createLoan(3L, false, 12, true));
  }

  private Loan createLoan(Long id, boolean isPaid, int installments, boolean hasOverdue) {
    Loan loan = new Loan();
    loan.setId(id);
    loan.setIsPaid(isPaid);
    loan.setNumberOfInstallment(installments);

    Set<LoanInstallment> loanInstallments = new HashSet<>();
    LocalDateTime now = LocalDateTime.now();

    for (int i = 0; i < installments; i++) {
      LoanInstallment installment = new LoanInstallment();
      installment.setIsPaid(isPaid);
      // If hasOverdue is true, set some installments to be overdue
      installment.setDueDate(
          hasOverdue && i < 2 ? now.minus(1, ChronoUnit.MONTHS) : now.plus(i, ChronoUnit.MONTHS));
      loanInstallments.add(installment);
    }

    loan.setInstallments(loanInstallments);
    return loan;
  }

  private boolean hasOverdueInstallments(Loan loan) {
    return loan.getInstallments().stream()
        .anyMatch(
            installment ->
                !installment.getIsPaid() && installment.getDueDate().isBefore(LocalDateTime.now()));
  }

  private Loan createLoanWithOrderedInstallments(Long id, int numberOfInstallments) {
    Loan loan = new Loan();
    loan.setId(id);
    loan.setIsPaid(false);
    loan.setNumberOfInstallment(numberOfInstallments);

    // Use TreeSet with custom comparator to maintain order by dueDate
    Set<LoanInstallment> installments =
        new TreeSet<>(Comparator.comparing(LoanInstallment::getDueDate));

    LocalDateTime startDate = LocalDateTime.now();

    for (int i = 0; i < numberOfInstallments; i++) {
      LoanInstallment installment = new LoanInstallment();
      installment.setId((long) (i + 1));
      installment.setAmount(BigDecimal.valueOf(1000));
      installment.setPaidAmount(BigDecimal.ZERO);
      installment.setDueDate(startDate.plusMonths(i));
      installment.setIsPaid(false);
      installments.add(installment);
    }

    loan.setInstallments(installments);
    return loan;
  }

  private Loan createLoanWithInstallments(Long id, BigDecimal installmentAmount, int count) {
    Loan loan = new Loan();
    loan.setId(id);
    loan.setIsPaid(false);
    loan.setNumberOfInstallment(count);
    loan.setLoanAmount(installmentAmount.multiply(BigDecimal.valueOf(count)));

    // Create and set customer
    Customer customer = new Customer();
    customer.setId(1L);
    customer.setUsedCreditLimit(loan.getLoanAmount());
    loan.setCustomer(customer);

    Set<LoanInstallment> installments =
        new TreeSet<>(Comparator.comparing(LoanInstallment::getDueDate));

    LocalDateTime startDate = LocalDateTime.now();

    for (int i = 0; i < count; i++) {
      LoanInstallment installment = new LoanInstallment();
      installment.setId((long) (i + 1));
      installment.setAmount(installmentAmount);
      installment.setPaidAmount(BigDecimal.ZERO);
      installment.setDueDate(startDate.plusMonths(i));
      installment.setIsPaid(false);
      installment.setLoan(loan);
      installments.add(installment);
    }

    loan.setInstallments(installments);
    return loan;
  }
}
