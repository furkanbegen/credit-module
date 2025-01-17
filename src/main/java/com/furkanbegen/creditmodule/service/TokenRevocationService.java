package com.furkanbegen.creditmodule.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class TokenRevocationService {

  private static final Set<String> tokenBlacklist = ConcurrentHashMap.newKeySet();

  public void addTokenToBlacklist(String token) {
    tokenBlacklist.add(token);
  }

  public boolean isTokenBlacklisted(String token) {
    return tokenBlacklist.contains(token);
  }
}
