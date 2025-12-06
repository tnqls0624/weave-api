package com.weave.domain.reaction.controller;

import com.weave.domain.reaction.dto.ReactionDto;
import com.weave.domain.reaction.dto.ReactionResponseDto;
import com.weave.domain.reaction.dto.ReactionSummaryDto;
import com.weave.domain.reaction.service.ReactionService;
import com.weave.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Reaction", description = "리액션 API")
@RestController
@RequestMapping
@RequiredArgsConstructor
public class ReactionController {

  private final ReactionService reactionService;

  // ===== 일정 리액션 =====

  @Operation(summary = "일정 리액션 조회", description = "일정의 리액션 목록을 조회합니다")
  @SecurityRequirement(name = "JWT")
  @GetMapping("/schedule/{scheduleId}/reactions")
  public ApiResponse<ReactionSummaryDto> getScheduleReactions(
      @PathVariable String scheduleId,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    log.info("Getting schedule reactions - scheduleId: {}, user: {}", scheduleId, userDetails.getUsername());
    try {
      ReactionSummaryDto result = reactionService.getScheduleReactions(scheduleId, userDetails.getUsername());
      log.info("Successfully retrieved reactions for schedule: {}", scheduleId);
      return ApiResponse.ok(result);
    } catch (Exception e) {
      log.error("Error retrieving schedule reactions - scheduleId: {}, error: {}", scheduleId, e.getMessage(), e);
      throw e;
    }
  }

  @Operation(summary = "일정 리액션 토글", description = "일정에 리액션을 추가하거나 제거합니다")
  @SecurityRequirement(name = "JWT")
  @PostMapping("/schedule/{scheduleId}/reactions")
  public ApiResponse<ReactionSummaryDto> toggleScheduleReaction(
      @PathVariable String scheduleId,
      @Valid @RequestBody ReactionDto dto,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    return ApiResponse.ok(reactionService.toggleScheduleReaction(scheduleId, dto.getEmoji(), userDetails.getUsername()));
  }

  // ===== 댓글 리액션 =====

  @Operation(summary = "댓글 리액션 조회", description = "댓글의 리액션 목록을 조회합니다")
  @SecurityRequirement(name = "JWT")
  @GetMapping("/comments/{commentId}/reactions")
  public ApiResponse<List<ReactionResponseDto>> getCommentReactions(
      @PathVariable String commentId,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    return ApiResponse.ok(reactionService.getCommentReactions(commentId, userDetails.getUsername()));
  }

  @Operation(summary = "댓글 리액션 토글", description = "댓글에 리액션을 추가하거나 제거합니다")
  @SecurityRequirement(name = "JWT")
  @PostMapping("/comments/{commentId}/reactions")
  public ApiResponse<Long> toggleCommentReaction(
      @PathVariable String commentId,
      @Valid @RequestBody ReactionDto dto,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    return ApiResponse.ok(reactionService.toggleCommentReaction(commentId, dto.getEmoji(), userDetails.getUsername()));
  }
}
