package com.lovechedule.domain.user.dto;

import lombok.*;

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
