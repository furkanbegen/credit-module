package com.furkanbegen.creditmodule.service;

import com.furkanbegen.creditmodule.security.SecurityUser;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JWTService {

  private final TokenResolver tokenResolver;
  private final JwtEncoder jwtEncoder;

  public JWTService(TokenResolver tokenResolver, final JwtEncoder jwtEncoder) {
    this.tokenResolver = tokenResolver;
    this.jwtEncoder = jwtEncoder;
  }

  public String generateToken(Authentication authentication) {
    var now = Instant.now();
    var claims =
        JwtClaimsSet.builder()
            .issuer("self")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(tokenResolver.getTokenExpirationDuration().getSeconds()))
            .subject(authentication.getName())
            .claim("user_id", ((SecurityUser) authentication.getPrincipal()).getId())
            .build();

    var encoderParameters =
        JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS512).build(), claims);
    return this.jwtEncoder.encode(encoderParameters).getTokenValue();
  }
}
