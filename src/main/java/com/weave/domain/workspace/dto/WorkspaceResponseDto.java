package com.weave.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.bson.types.ObjectId;

import com.weave.domain.workspace.entity.Workspace;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceResponseDto {

    @Schema(description = "워크스페이스 ID", example = "672cb481f2a04d291bbd1423")
    private ObjectId id;

    @Schema(description = "워크스페이스 제목", example = "우리 커플 일정")
    private String title;

    @Schema(description = "초대 코드", example = "ASD213")
    private String inviteCode;

    @Schema(description = "마스터 유저 정보")
    private ObjectId master;

    @Schema(description = "워크스페이스 참여 유저 목록")
    private List<ObjectId> users;

    @Schema(
            description = "태그 정보",
            example = """
        {
          "anniversary": { "color": "#FF0000" },
          "together": { "color": "#00FF00" },
          "guest": { "color": "#0000FF" },
          "master": { "color": "#FF00FF" }
        }
        """
    )
    private Workspace.Tag tags;

    @Schema(description = "사귄 날짜", example = "2024-01-01")
    private String loveDay;

    @Schema(description = "썸네일 이미지", example = "http://test.com/image")
    private String thumbnailImage;

    @Schema(description = "생성일시")
    private Date createdAt;

    @Schema(description = "수정일시")
    private Date updatedAt;
}
