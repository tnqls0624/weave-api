package com.lovechedule.domain.workspace.dto;

import com.lovechedule.domain.workspace.entity.Workspace;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWorkspaceRequestDto {

    @Schema(example = "ASD213", description = "초대코드")
    @NotBlank
    private String inviteCode;

    @Schema(example = "2024-01-01", description = "사귀기 시작한 날짜")
    @NotBlank
    private String loveDay;

    @Schema(
            description = "태그",
            example = """
        {
            "anniversary": { "color": "#FF0000" },
            "together": { "color": "#00FF00" },
            "guest": { "color": "#0000FF" },
            "master": { "color": "#FF00FF" }
        }
        """
    )
    @NotNull
    private Workspace.Tag tags;

    @Schema(example = "http://test.com/image", description = "썸네일 이미지")
    @NotBlank
    private String thumbnailImage;
}
