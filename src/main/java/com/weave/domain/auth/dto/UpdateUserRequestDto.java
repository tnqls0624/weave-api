package com.weave.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequestDto {

  @NotBlank(message = "이름은 필수 입력 항목입니다.")
  @Size(max = 10, message = "이름은 최대 10자까지 입력 가능합니다.")
  private String name;

  private String fcmToken;
  private boolean pushEnabled;
  private boolean scheduleAlarm;
  private boolean anniversaryAlarm;
}
