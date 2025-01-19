package com.furkanbegen.creditmodule.service.impl;

import com.furkanbegen.creditmodule.dto.CreateLoanRequest;
import com.furkanbegen.creditmodule.dto.LoanFilterDTO;
import com.furkanbegen.creditmodule.exception.InsufficientCreditLimitException;
import com.furkanbegen.creditmodule.model.Customer;
import com.furkanbegen.creditmodule.model.Loan;
import com.furkanbegen.creditmodule.model.LoanInstallment;
import com.furkanbegen.creditmodule.repository.CustomerRepository;
import com.furkanbegen.creditmodule.repository.LoanRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoanService {

  private final CustomerRepository customerRepository;
  private final LoanRepository loanRepository;

  @Transactional
  public Loan createLoan(Long customerId, CreateLoanRequest request) {
    Customer customer =
        customerRepository
            .findById(customerId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

    // Calculate total amount with interest - this will be our loan amount
    BigDecimal loanAmountWithInterest =
        request
            .getLoanAmount()
            .multiply(BigDecimal.ONE.add(request.getInterestRate()))
            .setScale(2, RoundingMode.HALF_UP);

    // Check credit limit against the total amount
    BigDecimal availableCredit = customer.getCreditLimit().subtract(customer.getUsedCreditLimit());
    if (availableCredit.compareTo(loanAmountWithInterest) < 0) {
      throw new InsufficientCreditLimitException("Insufficient credit limit");
    }

    // Create loan
    Loan loan = new Loan();
    loan.setCustomer(customer);
    loan.setLoanAmount(loanAmountWithInterest); // Store the total amount including interest
    loan.setNumberOfInstallment(request.getNumberOfInstallment().getValue());
    loan.setCreateDate(LocalDateTime.now());
    loan.setIsPaid(false);

    // Calculate installment amount
    BigDecimal installmentAmount =
        loanAmountWithInterest.divide(
            BigDecimal.valueOf(request.getNumberOfInstallment().getValue()),
            2,
            RoundingMode.HALF_UP);

    // Create installments with due dates on first day of each month
    Set<LoanInstallment> installments = new HashSet<>();
    LocalDateTime firstDueDate = getFirstDayOfNextMonth(loan.getCreateDate());

    for (int i = 0; i < request.getNumberOfInstallment().getValue(); i++) {
      LoanInstallment installment = new LoanInstallment();
      installment.setLoan(loan);
      installment.setAmount(installmentAmount);
      installment.setPaidAmount(BigDecimal.ZERO);
      installment.setDueDate(firstDueDate.plusMonths(i));
      installment.setIsPaid(false);
      installments.add(installment);
    }

    loan.setInstallments(installments);

    // Update customer's used credit limit with total amount (including interest)
    customer.setUsedCreditLimit(customer.getUsedCreditLimit().add(loanAmountWithInterest));
    customerRepository.save(customer);

    return loanRepository.save(loan);
  }

  private LocalDateTime getFirstDayOfNextMonth(LocalDateTime date) {
    return date.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
  }

  @Transactional(readOnly = true)
  public List<Loan> getLoans(Long customerId, LoanFilterDTO filter) {
    if (!customerRepository.existsById(customerId)) {
      throw new EntityNotFoundException("Customer not found");
    }

    return loanRepository.findLoansWithFilters(
        customerId,
        filter != null ? filter.getIsPaid() : null,
        filter != null && filter.getNumberOfInstallment() != null
            ? filter.getNumberOfInstallment().getValue()
            : null,
        filter != null ? filter.getIsOverdue() : null,
        LocalDateTime.now());
  }

  @Transactional(readOnly = true)
  public Loan getLoanWithInstallments(Long customerId, Long loanId) {
    return loanRepository
        .findByIdAndCustomerId(loanId, customerId)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    String.format(
                        "Loan not found with id: %d for customer: %d", loanId, customerId)));
  }
}
