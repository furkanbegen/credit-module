package com.furkanbegen.creditmodule.security;

import com.furkanbegen.creditmodule.model.Role;
import com.furkanbegen.creditmodule.model.User;
import com.furkanbegen.creditmodule.repository.RoleRepository;
import com.furkanbegen.creditmodule.repository.UserRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InsertUserComponent implements CommandLineRunner {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(final String... args) {

    var roles = roleRepository.findAll();

    if (roles.isEmpty()) {
      var adminRole = new Role();
      adminRole.setName("ROLE_ADMIN");

      var customerRole = new Role();
      customerRole.setName("ROLE_CUSTOMER");

      roleRepository.saveAll(List.of(adminRole, customerRole));
    }

    var users = userRepository.findAll();

    if (users.isEmpty()) {
      var adminUser = new User();
      adminUser.setEmail("admin@test.com");
      adminUser.setPassword(passwordEncoder.encode("123456"));
      adminUser.setName("Test");
      adminUser.setSurname("Admin");
      adminUser.setRoles(Set.of(roleRepository.findByName("ROLE_ADMIN").get()));

      var customerUser = new User();
      customerUser.setEmail("customer@test.com");
      customerUser.setPassword(passwordEncoder.encode("123456"));
      customerUser.setName("Test");
      customerUser.setSurname("Customer");
      customerUser.setRoles(Set.of(roleRepository.findByName("ROLE_CUSTOMER").get()));

      userRepository.saveAll(List.of(adminUser, customerUser));
    }
  }
}
