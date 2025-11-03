package com.lovechedule.domain.workspace.controller;

import com.lovechedule.domain.workspace.dto.CreateWorkspaceRequestDto;
import com.lovechedule.domain.workspace.dto.UpdateWorkspaceRequestDto;
import com.lovechedule.domain.workspace.dto.WorkspaceResponseDto;
import com.lovechedule.domain.workspace.service.WorkspaceService;
import com.lovechedule.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspace")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    // 워크스페이스 생성
    @SecurityRequirement(name = "JWT")
    @Tag(name = "WORKSPACE")
    @Operation(summary = "워크스페이스 생성")
    @PostMapping("/")
    public ApiResponse<WorkspaceResponseDto> createWorkspace(@Valid @RequestBody CreateWorkspaceRequestDto dto,
                                                             @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.ok(workspaceService.createWorkspace(dto, userDetails.getUsername()));
    }

    // 워크스페이스 수정
    @SecurityRequirement(name = "JWT")
    @Tag(name = "WORKSPACE")
    @Operation(summary = "워크스페이스 수정")
    @PutMapping("/{id}")
    public ApiResponse<WorkspaceResponseDto> updateWorkspace(@Valid @RequestBody UpdateWorkspaceRequestDto dto,
                                                             @PathVariable("id") String id) {
        return ApiResponse.ok(workspaceService.updateWorkspace(dto, id));
    }


}
