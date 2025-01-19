package com.furkanbegen.creditmodule.repository;

import com.furkanbegen.creditmodule.model.Loan;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

  @Query(
      """
        SELECT DISTINCT l FROM Loan l
        LEFT JOIN FETCH l.installments i
        WHERE l.customer.id = :customerId
        AND (:isPaid IS NULL OR l.isPaid = :isPaid)
        AND (:numberOfInstallment IS NULL OR l.numberOfInstallment = :numberOfInstallment)
        AND (:isOverdue IS NULL OR
            (:isOverdue = true AND EXISTS (
                SELECT 1 FROM LoanInstallment li
                WHERE li.loan = l
                AND li.isPaid = false
                AND li.dueDate < :currentDate
            )))
        """)
  List<Loan> findLoansWithFilters(
      @Param("customerId") Long customerId,
      @Param("isPaid") Boolean isPaid,
      @Param("numberOfInstallment") Integer numberOfInstallment,
      @Param("isOverdue") Boolean isOverdue,
      @Param("currentDate") LocalDateTime currentDate);
}
