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
  // 부분 업데이트를 위해 박싱 타입 사용: null이면 갱신하지 않음
  private Boolean pushEnabled;
  private String avatarUrl;
}
