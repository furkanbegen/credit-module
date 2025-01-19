package com.furkanbegen.creditmodule.security;

import com.furkanbegen.creditmodule.repository.CustomerRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Slf4j
@Component("customerSecurity")
@RequiredArgsConstructor
public class CustomerSecurityEvaluator {

  private final CustomerRepository customerRepository;

  public boolean hasAccess(Authentication authentication, Long customerId) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }

    // Admin has access to all customers
    if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
      return true;
    }

    // For CUSTOMER role, check if the customer belongs to the authenticated user
    if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CUSTOMER"))) {
      Long userId =
          Optional.ofNullable(authentication.getPrincipal())
              .filter(Jwt.class::isInstance)
              .map(Jwt.class::cast)
              .map(jwt -> jwt.getClaim("user_id"))
              .map(id -> (Long) id)
              .orElse(null);

      if (userId == null) {
        return false;
      }

      return customerRepository
          .findByUserId(userId)
          .map(customer -> customerId.equals(customer.getId()))
          .orElse(false);
    }

    return false;
  }
}
