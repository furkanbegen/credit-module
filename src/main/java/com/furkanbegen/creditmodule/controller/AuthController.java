package com.furkanbegen.creditmodule.controller;

import static com.furkanbegen.creditmodule.constant.AppConstant.API_BASE_PATH;

import com.furkanbegen.creditmodule.dto.AuthRequestDTO;
import com.furkanbegen.creditmodule.dto.AuthResponseDTO;
import com.furkanbegen.creditmodule.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;

@RequiredArgsConstructor
@RestController
@RequestMapping(API_BASE_PATH)
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  public ResponseEntity<AuthResponseDTO> authenticateAndGetToken(
      @Valid @RequestBody AuthRequestDTO authRequest) {
    return ResponseEntity.ok(authService.authenticateAndGetToken(authRequest));
  }

  @PostMapping("/logout")
  public ResponseEntity<String> logout(HttpServletRequest request) {
    authService.logout(request);
    return ResponseEntity.ok().build();
  }
}
