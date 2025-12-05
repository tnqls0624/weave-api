package com.weave.domain.comment.controller;

import com.weave.domain.comment.dto.CommentResponseDto;
import com.weave.domain.comment.dto.CreateCommentDto;
import com.weave.domain.comment.dto.UpdateCommentDto;
import com.weave.domain.comment.service.CommentService;
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

@Tag(name = "Schedule Comment", description = "일정 댓글 API")
@RestController
@RequestMapping("/schedule/{scheduleId}/comments")
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;

  @Operation(summary = "댓글 목록 조회", description = "일정의 댓글 목록을 조회합니다")
  @SecurityRequirement(name = "JWT")
  @GetMapping
  public ApiResponse<List<CommentResponseDto>> getComments(@PathVariable String scheduleId) {
    return ApiResponse.ok(commentService.getCommentsByScheduleId(scheduleId));
  }

  @Operation(summary = "댓글 작성", description = "일정에 댓글을 작성합니다")
  @SecurityRequirement(name = "JWT")
  @PostMapping
  public ApiResponse<CommentResponseDto> createComment(
      @PathVariable String scheduleId,
      @Valid @RequestBody CreateCommentDto dto,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    return ApiResponse.ok(commentService.createComment(scheduleId, dto, userDetails.getUsername()));
  }

  @Operation(summary = "댓글 수정", description = "댓글을 수정합니다")
  @SecurityRequirement(name = "JWT")
  @PutMapping("/{commentId}")
  public ApiResponse<CommentResponseDto> updateComment(
      @PathVariable String scheduleId,
      @PathVariable String commentId,
      @Valid @RequestBody UpdateCommentDto dto,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    return ApiResponse.ok(commentService.updateComment(commentId, dto, userDetails.getUsername()));
  }

  @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다")
  @SecurityRequirement(name = "JWT")
  @DeleteMapping("/{commentId}")
  public ApiResponse<Void> deleteComment(
      @PathVariable String scheduleId,
      @PathVariable String commentId,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    commentService.deleteComment(commentId, userDetails.getUsername());
    return ApiResponse.ok(null);
  }
}
