package com.weave.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateNotificationRequestDto {

  private Boolean pushEnabled;
  private String fcmToken;
  private Boolean locationEnabled;
}
