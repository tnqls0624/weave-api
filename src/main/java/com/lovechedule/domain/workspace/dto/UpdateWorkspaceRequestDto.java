package com.lovechedule.domain.workspace.dto;

import com.lovechedule.domain.workspace.entity.Workspace;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class UpdateWorkspaceRequestDto {

    @Schema(
            example = "2024-01-01",
            description = "사귀기 시작한 날짜"
    )
    private String loveDay;

    @Schema(example = "[\"68576234782d9c909f1b5ff2\", \"68576234782d9c909f1b5ff3\"]")
    private List<String> users;

    @Schema(
            example = """
        {
          "anniversary": { "color": "#FF0000" },
          "together": { "color": "#00FF00" },
          "guest": { "color": "#0000FF" },
          "master": { "color": "#FF00FF" }
        }
        """,
            description = "태그"
    )
    private Workspace.Tag tags;

    @Schema(
            example = "http://test.com/image",
            description = "썸네일 이미지"
    )
    private String thumbnailImage;
}
