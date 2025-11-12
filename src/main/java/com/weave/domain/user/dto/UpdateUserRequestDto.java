package com.weave.domain.user.dto;

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

  @Size(max = 10, message = "이름은 최대 10자까지 입력 가능합니다.")
  private String name;

  private String fcmToken;
  private Boolean pushEnabled;
  private String avatarUrl;
}
