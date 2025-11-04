package com.weave.domain.workspace.dto;

import com.weave.domain.workspace.entity.Workspace;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import java.util.List;
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

  @Schema(description = "마스터 유저 정보")
  private ObjectId master;

  @Schema(description = "워크스페이스 참여 유저 목록")
  private List<ObjectId> users;

  @Schema(description = "사귄 날짜", example = "2024-01-01")
  private String loveDay;

  @Schema(description = "썸네일 이미지", example = "http://test.com/image")
  private String thumbnailImage;

  @Schema(description = "생성일시")
  private Date createdAt;

  @Schema(description = "수정일시")
  private Date updatedAt;

  public static WorkspaceResponseDto from(Workspace workspace) {
    return WorkspaceResponseDto.builder()
        .id(workspace.getId())
        .master(workspace.getMaster())
        .users(workspace.getUsers())
        .loveDay(workspace.getLoveDay())
        .thumbnailImage(workspace.getThumbnailImage())
        .createdAt(workspace.getCreatedAt())
        .updatedAt(workspace.getUpdatedAt())
        .build();
  }
}
