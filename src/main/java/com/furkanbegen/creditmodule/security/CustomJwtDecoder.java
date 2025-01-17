package com.furkanbegen.creditmodule.security;

import com.furkanbegen.creditmodule.service.TokenRevocationService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
public class CustomJwtDecoder implements JwtDecoder {

  private final JwtDecoder delegate;
  private final TokenRevocationService tokenRevocationService;

  public CustomJwtDecoder(
      final JwtDecoder delegate, final TokenRevocationService tokenRevocationService) {
    this.delegate = delegate;
    this.tokenRevocationService = tokenRevocationService;
  }

  @Override
  public Jwt decode(final String token) throws JwtException {
    var decodedToken = delegate.decode(token);
    if (tokenRevocationService.isTokenBlacklisted(decodedToken.getTokenValue())) {
      throw new JwtException("Token is revoked");
    }
    return decodedToken;
  }
}
