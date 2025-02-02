package com.furkanbegen.creditmodule.repository;

import com.furkanbegen.creditmodule.model.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
  Optional<Customer> findByUserId(Long userId);
}
