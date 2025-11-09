package com.weave.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestTokenRequestDto {

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  private String email;
}
