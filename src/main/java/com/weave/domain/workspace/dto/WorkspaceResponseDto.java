package com.weave.domain.workspace.dto;

import com.google.common.collect.ImmutableMap;
import com.weave.domain.user.dto.UserResponseDto;
import com.weave.domain.workspace.entity.Workspace;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceResponseDto {

  @Schema(description = "워크스페이스 ID", example = "672cb481f2a04d291bbd1423")
  private ObjectId id;

  @Schema(description = "워크스페이스 이름", example = "우리들의 다이어리")
  private String title;

  @Schema(description = "마스터 유저 정보")
  private UserResponseDto master;

  @Schema(description = "워크스페이스 참여 유저 목록")
  private List<UserResponseDto> users;

  @Schema(description = "참여자별 스케줄 태그 색상 (userId: colorCode)", example = "{\"userId1\": \"#FF5733\", \"userId2\": \"#33C3FF\"}")
  private Map<String, String> participantColors;

  @Schema(description = "사귄 날짜", example = "2024-01-01")
  private String loveDay;

  @Schema(description = "썸네일 이미지", example = "http://test.com/image")
  private String thumbnailImage;

  @Schema(description = "생성일시")
  private Date createdAt;

  @Schema(description = "수정일시")
  private Date updatedAt;

  public static WorkspaceResponseDto fromEntityOnly(Workspace workspace) {
    // 엔티티 필드만 채워진 기본 빌더 (유저 join 전 단계)
    return WorkspaceResponseDto.builder()
        .id(workspace.getId())
        .title(workspace.getTitle())
        .participantColors(workspace.getParticipantColors() != null
            ? workspace.getParticipantColors()
            : ImmutableMap.of())
        .loveDay(workspace.getLoveDay())
        .thumbnailImage(workspace.getThumbnailImage())
        .createdAt(workspace.getCreatedAt())
        .updatedAt(workspace.getUpdatedAt())
        .build();
  }
}
