package com.furkanbegen.creditmodule.service.impl;

import com.furkanbegen.creditmodule.dto.CreateLoanRequest;
import com.furkanbegen.creditmodule.dto.LoanFilterDTO;
import com.furkanbegen.creditmodule.dto.LoanPaymentRequest;
import com.furkanbegen.creditmodule.dto.LoanPaymentResponse;
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
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
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

  private static final BigDecimal DAILY_RATE = BigDecimal.valueOf(0.001);
  private static final int MAX_MONTHS_AHEAD = 3;

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
    loan.setInterestRate(request.getInterestRate());
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

  @Transactional
  public LoanPaymentResponse payLoan(Long customerId, Long loanId, LoanPaymentRequest request) {
    Loan loan = getLoanWithInstallments(customerId, loanId);

    if (loan.getIsPaid()) {
      throw new IllegalStateException("Loan is already fully paid");
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime maxPayableDate = now.plusMonths(MAX_MONTHS_AHEAD);

    List<LoanInstallment> payableInstallments =
        loan.getInstallments().stream()
            .filter(installment -> !installment.getIsPaid())
            .filter(installment -> installment.getDueDate().isBefore(maxPayableDate))
            .sorted(Comparator.comparing(LoanInstallment::getDueDate))
            .toList();

    if (payableInstallments.isEmpty()) {
      throw new IllegalStateException("No payable installments found");
    }

    BigDecimal remainingPayment = request.getPaymentAmount();
    int installmentsPaid = 0;
    BigDecimal totalPaid = BigDecimal.ZERO;
    BigDecimal totalDiscount = BigDecimal.ZERO;
    BigDecimal totalPenalty = BigDecimal.ZERO;

    for (LoanInstallment installment : payableInstallments) {
      BigDecimal adjustedAmount = calculateAdjustedAmount(installment, now);

      if (remainingPayment.compareTo(adjustedAmount) >= 0) {
        // Can pay this installment
        installment.setIsPaid(true);
        installment.setPaidAmount(adjustedAmount);
        installment.setPaymentDate(now);

        remainingPayment = remainingPayment.subtract(adjustedAmount);
        installmentsPaid++;
        totalPaid = totalPaid.add(adjustedAmount);

        // Calculate discount or penalty
        BigDecimal adjustment = adjustedAmount.subtract(installment.getAmount());
        if (adjustment.compareTo(BigDecimal.ZERO) < 0) {
          totalDiscount = totalDiscount.add(adjustment.abs());
        } else {
          totalPenalty = totalPenalty.add(adjustment);
        }
      } else {
        break;
      }
    }

    if (installmentsPaid == 0) {
      throw new IllegalArgumentException("Payment amount is insufficient for any installment");
    }

    // Check if loan is fully paid
    boolean isFullyPaid = loan.getInstallments().stream().allMatch(LoanInstallment::getIsPaid);

    if (isFullyPaid) {
      loan.setIsPaid(true);

      // Update customer's used credit limit
      Customer customer = loan.getCustomer();
      customer.setUsedCreditLimit(customer.getUsedCreditLimit().subtract(loan.getLoanAmount()));
      customerRepository.save(customer);
    }

    loanRepository.save(loan);

    return LoanPaymentResponse.builder()
        .numberOfInstallmentsPaid(installmentsPaid)
        .totalAmountPaid(totalPaid)
        .isLoanFullyPaid(isFullyPaid)
        .totalDiscount(totalDiscount)
        .totalPenalty(totalPenalty)
        .build();
  }

  private BigDecimal calculateAdjustedAmount(
      LoanInstallment installment, LocalDateTime paymentDate) {
    long daysDifference =
        ChronoUnit.DAYS.between(installment.getDueDate().toLocalDate(), paymentDate.toLocalDate());

    if (daysDifference == 0) {
      return installment.getAmount();
    }

    BigDecimal adjustmentRate = DAILY_RATE.multiply(BigDecimal.valueOf(Math.abs(daysDifference)));
    BigDecimal adjustment = installment.getAmount().multiply(adjustmentRate);

    if (daysDifference < 0) {
      // Payment before due date - apply discount
      return installment.getAmount().subtract(adjustment);
    } else {
      // Payment after due date - apply penalty
      return installment.getAmount().add(adjustment);
    }
  }
}
