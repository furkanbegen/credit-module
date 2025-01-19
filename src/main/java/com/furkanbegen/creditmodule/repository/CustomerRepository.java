package com.furkanbegen.creditmodule.repository;

import com.furkanbegen.creditmodule.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {}
