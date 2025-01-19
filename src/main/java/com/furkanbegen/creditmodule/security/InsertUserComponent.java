package com.furkanbegen.creditmodule.security;

import com.furkanbegen.creditmodule.model.Customer;
import com.furkanbegen.creditmodule.model.Role;
import com.furkanbegen.creditmodule.model.User;
import com.furkanbegen.creditmodule.repository.CustomerRepository;
import com.furkanbegen.creditmodule.repository.RoleRepository;
import com.furkanbegen.creditmodule.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InsertUserComponent implements CommandLineRunner {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final CustomerRepository customerRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(final String... args) {
    insertRoles();
    insertUsersAndCustomers();
  }

  private void insertRoles() {
    var roles = roleRepository.findAll();

    if (roles.isEmpty()) {
      var adminRole = new Role();
      adminRole.setName("ROLE_ADMIN");

      var customerRole = new Role();
      customerRole.setName("ROLE_CUSTOMER");

      roleRepository.saveAll(List.of(adminRole, customerRole));
    }
  }

  private void insertUsersAndCustomers() {
    var users = userRepository.findAll();

    if (users.isEmpty()) {
      // Create admin user
      var adminUser = new User();
      adminUser.setEmail("admin@test.com");
      adminUser.setPassword(passwordEncoder.encode("123456"));
      adminUser.setName("Test");
      adminUser.setSurname("Admin");
      adminUser.setRoles(Set.of(roleRepository.findByName("ROLE_ADMIN").get()));
      userRepository.save(adminUser);

      // Create customer user and associated customer
      var customerUser = new User();
      customerUser.setEmail("customer@test.com");
      customerUser.setPassword(passwordEncoder.encode("123456"));
      customerUser.setName("Test");
      customerUser.setSurname("Customer");
      customerUser.setRoles(Set.of(roleRepository.findByName("ROLE_CUSTOMER").get()));
      userRepository.save(customerUser);

      // Create customer entity linked to customer user
      var customer = new Customer();
      customer.setName(customerUser.getName());
      customer.setSurname(customerUser.getSurname());
      customer.setCreditLimit(BigDecimal.valueOf(100000));
      customer.setUsedCreditLimit(BigDecimal.ZERO);
      customer.setUser(customerUser);
      customerRepository.save(customer);

      var anotherUser = new User();
      anotherUser.setEmail("anotherUser@test.com");
      anotherUser.setPassword(passwordEncoder.encode("123456"));
      anotherUser.setName("Another");
      anotherUser.setSurname("User");
      anotherUser.setRoles(Set.of(roleRepository.findByName("ROLE_CUSTOMER").get()));
      userRepository.save(anotherUser);

      var anotherCustomerUser = new Customer();
      anotherCustomerUser.setName("Another");
      anotherCustomerUser.setSurname("Customer");
      anotherCustomerUser.setCreditLimit(BigDecimal.valueOf(100000));
      anotherCustomerUser.setUsedCreditLimit(BigDecimal.ZERO);
      anotherCustomerUser.setUser(anotherUser);
      customerRepository.save(anotherCustomerUser);
    }
  }
}
