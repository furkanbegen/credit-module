package com.furkanbegen.creditmodule.repository;

import com.furkanbegen.creditmodule.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {}
