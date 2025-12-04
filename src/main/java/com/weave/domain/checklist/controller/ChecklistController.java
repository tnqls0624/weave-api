package com.weave.domain.checklist.controller;

import com.weave.domain.checklist.dto.ChecklistItemDto;
import com.weave.domain.checklist.dto.CreateChecklistItemDto;
import com.weave.domain.checklist.service.ChecklistService;
import com.weave.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schedule/{scheduleId}/checklist")
@RequiredArgsConstructor
@Tag(name = "Checklist", description = "일정 체크리스트 API")
public class ChecklistController {

  private final ChecklistService checklistService;

  @SecurityRequirement(name = "JWT")
  @Operation(summary = "체크리스트 조회")
  @GetMapping
  public ApiResponse<List<ChecklistItemDto>> getChecklist(@PathVariable String scheduleId) {
    return ApiResponse.ok(checklistService.getChecklist(scheduleId));
  }

  @SecurityRequirement(name = "JWT")
  @Operation(summary = "체크리스트 항목 추가")
  @PostMapping
  public ApiResponse<ChecklistItemDto> addItem(
      @PathVariable String scheduleId,
      @Valid @RequestBody CreateChecklistItemDto dto,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    return ApiResponse.ok(checklistService.addItem(scheduleId, dto, userDetails.getUsername()));
  }

  @SecurityRequirement(name = "JWT")
  @Operation(summary = "체크리스트 항목 토글")
  @PutMapping("/{itemId}/toggle")
  public ApiResponse<ChecklistItemDto> toggleItem(
      @PathVariable String scheduleId,
      @PathVariable String itemId,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    return ApiResponse.ok(checklistService.toggleItem(scheduleId, itemId, userDetails.getUsername()));
  }

  @SecurityRequirement(name = "JWT")
  @Operation(summary = "체크리스트 항목 삭제")
  @DeleteMapping("/{itemId}")
  public ApiResponse<Void> deleteItem(
      @PathVariable String scheduleId,
      @PathVariable String itemId
  ) {
    checklistService.deleteItem(scheduleId, itemId);
    return ApiResponse.ok(null);
  }
}
