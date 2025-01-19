package com.furkanbegen.creditmodule.security;

import com.furkanbegen.creditmodule.model.Role;
import com.furkanbegen.creditmodule.repository.UserRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CustomJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  private final UserRepository userRepository;

  public CustomJwtAuthenticationConverter(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    Long userId = jwt.getClaim("user_id");

    SecurityUser user =
        userRepository
            .findById(userId)
            .map(
                u ->
                    new SecurityUser(
                        u.getId(),
                        u.getEmail(),
                        u.getPassword(),
                        u.getName(),
                        u.getSurname(),
                        getAuthorities(u.getRoles())))
            .orElse(null);

    Collection<GrantedAuthority> authorities =
        user != null
            ? (Collection<GrantedAuthority>) user.getAuthorities()
            : Collections.emptyList();
    return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
  }

  private Collection<GrantedAuthority> getAuthorities(Collection<Role> roles) {
    Set<GrantedAuthority> authorities = new HashSet<>();

    // Add role-based authorities
    roles.forEach(
        role -> authorities.add(new SimpleGrantedAuthority(role.getName().toUpperCase())));

    return authorities;
  }
}
