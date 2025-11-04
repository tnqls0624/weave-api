package com.weave.domain.user.dto;

import com.weave.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {

  @Schema(description = "사용자 ID", example = "672cb481f2a04d291bbd1423")
  private ObjectId id;

  @Schema(description = "로그인 타입", example = "google")
  private String loginType;

  @Schema(description = "이메일 주소", example = "test@example.com")
  private String email;

  @Schema(description = "사용자 이름", example = "이수빈")
  private String name;

  @Schema(description = "성별", example = "female")
  private String gender;

  @Schema(description = "생일", example = "1993-07-11")
  private String birthday;

  @Schema(description = "프로필 썸네일 URL", example = "https://example.com/image.jpg")
  private String thumbnailImage;

  @Schema(description = "초대 코드", example = "ABC123")
  private String inviteCode;

  @Schema(description = "FCM 토큰")
  private String fcmToken;

  @Schema(description = "푸시 알림 허용 여부", example = "true")
  private boolean pushEnabled;

//    @Schema(description = "기념일 알림 여부", example = "true")
//    private boolean anniversaryAlarm;
//
//    @Schema(description = "일정 알림 여부", example = "false")
//    private boolean scheduleAlarm;

  @Schema(description = "생성일시", example = "2025-01-01T10:00:00Z")
  private Date createdAt;

  @Schema(description = "수정일시", example = "2025-01-02T10:00:00Z")
  private Date updatedAt;

  public static UserResponseDto from(User user) {
    return UserResponseDto.builder()
        .id(user.getId())
        .loginType(user.getLoginType())
        .email(user.getEmail())
        .name(user.getName())
        .gender(user.getGender())
        .birthday(user.getBirthday())
        .thumbnailImage(user.getThumbnailImage())
        .inviteCode(user.getInviteCode())
        .fcmToken(user.getFcmToken())
        .pushEnabled(user.isPushEnabled())
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .build();
  }
}
