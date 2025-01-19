package com.furkanbegen.creditmodule.security;

import com.furkanbegen.creditmodule.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component("customerSecurity")
@RequiredArgsConstructor
public class TestCustomerSecurityEvaluator {

  private final CustomerRepository customerRepository;

  public boolean hasAccess(Authentication authentication, Long customerId) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }

    if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
      return true;
    }

    if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CUSTOMER"))) {
      return customerRepository
          .findByUserId(1L) // hardcoded for test
          .map(customer -> customer.getId().equals(customerId))
          .orElse(false);
    }

    return false;
  }
}
