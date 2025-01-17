package com.furkanbegen.creditmodule.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDTO {

  private String email;
  private String name;
  private String surname;
  private String accessToken;
}
