package com.weave.domain.workspace.controller;

import com.weave.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.weave.domain.workspace.dto.CreateWorkspaceRequestDto;
import com.weave.domain.workspace.dto.UpdateWorkspaceRequestDto;
import com.weave.domain.workspace.dto.WorkspaceResponseDto;
import com.weave.domain.workspace.service.WorkspaceService;
import com.weave.global.dto.ApiResponse;

@RestController
@RequestMapping("/workspace")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final AuthService authService;

    // 워크스페이스 생성
    @SecurityRequirement(name = "JWT")
    @Tag(name = "WORKSPACE")
    @Operation(summary = "워크스페이스 생성")
    @PostMapping("/")
    public ApiResponse<WorkspaceResponseDto> create(@Valid @RequestBody CreateWorkspaceRequestDto dto,
                                                             @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.ok(workspaceService.create(dto, userDetails.getUsername()));
    }

    // 워크스페이스 수정
    @SecurityRequirement(name = "JWT")
    @Tag(name = "WORKSPACE")
    @Operation(summary = "워크스페이스 수정")
    @PutMapping("/{id}")
    public ApiResponse<WorkspaceResponseDto> update(@Valid @RequestBody UpdateWorkspaceRequestDto dto,
                                                             @PathVariable("id") String id) {
        return ApiResponse.ok(workspaceService.update(dto, id));
    }

    // 워크스페이스 전부 찾기
    @SecurityRequirement(name = "JWT")
    @Tag(name = "WORKSPACE")
    @Operation(summary = "워크스페이스 찾기")
    @GetMapping("/")
    public ApiResponse<WorkspaceResponseDto[]> find(@AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.ok(workspaceService.find(userDetails.getUsername()));
    }

    // 워크스페이스 ID로 찾기
    @SecurityRequirement(name = "JWT")
    @Tag(name = "WORKSPACE")
    @Operation(summary = "워크스페이스 찾기")
    @GetMapping("/{id}")
    public ApiResponse<WorkspaceResponseDto> findById(@PathVariable("id") String id) {
        return ApiResponse.ok(workspaceService.findById(id));
    }


}
