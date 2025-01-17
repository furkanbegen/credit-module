package com.furkanbegen.creditmodule.security;

import com.furkanbegen.creditmodule.model.Role;
import com.furkanbegen.creditmodule.repository.UserRepository;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailService implements UserDetailsService {

  private final UserRepository userRepository;

  public UserDetailService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    var userOptional = userRepository.findByEmail(username);
    var securityUser =
        userOptional.orElseThrow(
            () -> new UsernameNotFoundException("User not found: " + username));

    return new SecurityUser(
        securityUser.getId(),
        securityUser.getEmail(),
        securityUser.getPassword(),
        securityUser.getName(),
        securityUser.getSurname(),
        getAuthorities(securityUser.getRoles()));
  }

  private Collection<? extends GrantedAuthority> getAuthorities(Collection<Role> roles) {
    return roles.stream()
        .map(role -> new SimpleGrantedAuthority(role.getName()))
        .collect(Collectors.toList());
  }
}
