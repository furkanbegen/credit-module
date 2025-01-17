package com.furkanbegen.creditmodule.service;

import com.furkanbegen.creditmodule.dto.AuthRequestDTO;
import com.furkanbegen.creditmodule.dto.AuthResponseDTO;
import com.furkanbegen.creditmodule.security.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final JWTService jwtService;
  private final TokenRevocationService tokenRevocationService;

  public AuthResponseDTO authenticateAndGetToken(AuthRequestDTO authRequest) {
    Authentication authentication = authenticateUser(authRequest);
    if (authentication.isAuthenticated()) {
      String accessToken = jwtService.generateToken(authentication);
      var principal = (SecurityUser) authentication.getPrincipal();

      return new AuthResponseDTO(
          principal.getEmail(), principal.getName(), principal.getSurname(), accessToken);
    } else {
      throw new UsernameNotFoundException("Invalid user authRequest..!!");
    }
  }

  private Authentication authenticateUser(AuthRequestDTO authRequest) {
    return authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword()));
  }

  public void logout(HttpServletRequest request) {
    String logoutToken = extractToken(request);
    if (logoutToken != null) {
      tokenRevocationService.addTokenToBlacklist(logoutToken);
    } else {
      throw new IllegalArgumentException("Invalid token");
    }
  }

  private String extractToken(HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token != null && token.startsWith("Bearer ")) {
      return token.substring(7);
    }
    return null;
  }
}
