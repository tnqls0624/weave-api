package com.weave.domain.user.dto;

import com.weave.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {

  @Schema(description = "사용자 ID", example = "672cb481f2a04d291bbd1423")
  private String id;

  @Schema(description = "로그인 타입", example = "google")
  private String loginType;

  @Schema(description = "이메일 주소", example = "test@example.com")
  private String email;

  @Schema(description = "사용자 이름", example = "이수빈")
  private String name;

  @Schema(description = "생일", example = "0624")
  private String birthday;

  @Schema(description = "초대 코드", example = "ABC123")
  private String inviteCode;

  @Schema(description = "FCM 토큰")
  private String fcmToken;

  @Schema(description = "푸시 알림 허용 여부", example = "true")
  private boolean pushEnabled;

  @Schema(description = "위치 사용 여부", example = "true")
  private boolean locationEnabled;

  @Schema(description = "피싱 가드 활성화 여부", example = "false")
  private boolean phishingGuardEnabled;

  @Schema(description = "피싱 자동 차단 여부", example = "false")
  private boolean phishingAutoBlock;

  @Schema(description = "피싱 검사 민감도", example = "medium")
  private String phishingSensitivityLevel;

  @Schema(description = "유저 프로필 이미지", example = "https://example.com/avatar.jpg")
  private String avatarUrl;

  @Schema(description = "생성일시", example = "2025-01-01T10:00:00Z")
  private Date createdAt;

  @Schema(description = "수정일시", example = "2025-01-02T10:00:00Z")
  private Date updatedAt;

  public static UserResponseDto from(User user) {
    return UserResponseDto.builder()
        .id(user.getId() != null ? user.getId().toHexString() : null)
        .loginType(user.getLoginType())
        .email(user.getEmail())
        .name(user.getName())
        .birthday(user.getBirthday())
        .inviteCode(user.getInviteCode())
        .fcmToken(user.getFcmToken())
        .pushEnabled(user.getPushEnabled())
        .locationEnabled(user.getLocationEnabled())
        .phishingGuardEnabled(user.getPhishingGuardEnabled() != null ? user.getPhishingGuardEnabled() : false)
        .phishingAutoBlock(user.getPhishingAutoBlock() != null ? user.getPhishingAutoBlock() : false)
        .phishingSensitivityLevel(user.getPhishingSensitivityLevel() != null ? user.getPhishingSensitivityLevel() : "medium")
        .avatarUrl(user.getAvatarUrl())
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .build();
  }
}
