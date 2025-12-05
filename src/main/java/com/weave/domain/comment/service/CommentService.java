package com.weave.domain.comment.service;

import com.weave.domain.comment.dto.CommentResponseDto;
import com.weave.domain.comment.dto.CreateCommentDto;
import com.weave.domain.comment.dto.UpdateCommentDto;
import com.weave.domain.comment.entity.Comment;
import com.weave.domain.comment.repository.CommentRepository;
import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;
  private final UserRepository userRepository;

  // 댓글 목록 조회
  @Transactional(readOnly = true)
  public List<CommentResponseDto> getCommentsByScheduleId(String scheduleId) {
    ObjectId scheduleObjectId = new ObjectId(scheduleId);
    List<Comment> comments = commentRepository.findByScheduleIdOrderByCreatedAtAsc(scheduleObjectId);

    // 작성자 정보를 한 번에 조회
    List<ObjectId> authorIds = comments.stream()
        .map(Comment::getAuthorId)
        .distinct()
        .collect(Collectors.toList());

    Map<ObjectId, User> userMap = userRepository.findAllById(authorIds).stream()
        .collect(Collectors.toMap(User::getId, user -> user));

    return comments.stream()
        .map(comment -> {
          User author = userMap.get(comment.getAuthorId());
          if (author != null) {
            return CommentResponseDto.from(comment, author.getName(), author.getAvatarUrl());
          }
          return CommentResponseDto.from(comment);
        })
        .collect(Collectors.toList());
  }

  // 댓글 작성
  @Transactional
  public CommentResponseDto createComment(String scheduleId, CreateCommentDto dto, String email) {
    User author = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

    Comment comment = Comment.builder()
        .scheduleId(new ObjectId(scheduleId))
        .content(dto.getContent())
        .authorId(author.getId())
        .build();

    Comment saved = commentRepository.save(comment);
    return CommentResponseDto.from(saved, author.getName(), author.getAvatarUrl());
  }

  // 댓글 수정
  @Transactional
  public CommentResponseDto updateComment(String commentId, UpdateCommentDto dto, String email) {
    Comment comment = commentRepository.findById(new ObjectId(commentId))
        .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다"));

    User author = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

    // 작성자 본인만 수정 가능
    if (!comment.getAuthorId().equals(author.getId())) {
      throw new IllegalArgumentException("댓글 수정 권한이 없습니다");
    }

    comment.setContent(dto.getContent());
    Comment saved = commentRepository.save(comment);
    return CommentResponseDto.from(saved, author.getName(), author.getAvatarUrl());
  }

  // 댓글 삭제
  @Transactional
  public void deleteComment(String commentId, String email) {
    Comment comment = commentRepository.findById(new ObjectId(commentId))
        .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다"));

    User author = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

    // 작성자 본인만 삭제 가능
    if (!comment.getAuthorId().equals(author.getId())) {
      throw new IllegalArgumentException("댓글 삭제 권한이 없습니다");
    }

    commentRepository.delete(comment);
  }
}
