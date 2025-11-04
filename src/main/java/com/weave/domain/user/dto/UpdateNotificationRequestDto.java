package com.weave.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateNotificationRequestDto {

  private String fcmToken;
  private boolean pushEnabled;
  private boolean scheduleAlarm;
  private boolean anniversaryAlarm;
}
