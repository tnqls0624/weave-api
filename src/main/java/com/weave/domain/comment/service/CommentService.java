package com.weave.domain.comment.service;

import com.weave.domain.comment.dto.CommentResponseDto;
import com.weave.domain.comment.dto.CommentResponseDto.MentionedUserDto;
import com.weave.domain.comment.dto.CreateCommentDto;
import com.weave.domain.comment.dto.UpdateCommentDto;
import com.weave.domain.comment.entity.Comment;
import com.weave.domain.comment.repository.CommentRepository;
import com.weave.domain.reaction.dto.ReactionResponseDto;
import com.weave.domain.reaction.entity.CommentReaction;
import com.weave.domain.reaction.repository.CommentReactionRepository;
import com.weave.domain.schedule.entity.Schedule;
import com.weave.domain.schedule.repository.ScheduleRepository;
import com.weave.domain.schedule.service.NotificationService;
import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
import java.util.ArrayList;
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
  private final CommentReactionRepository commentReactionRepository;
  private final UserRepository userRepository;
  private final ScheduleRepository scheduleRepository;
  private final NotificationService notificationService;

  // 댓글 목록 조회 (답글 포함)
  @Transactional(readOnly = true)
  public List<CommentResponseDto> getCommentsByScheduleId(String scheduleId, String email) {
    ObjectId scheduleObjectId = new ObjectId(scheduleId);

    // 부모 댓글만 조회
    List<Comment> parentComments = commentRepository.findByScheduleIdAndParentIdIsNullOrderByCreatedAtAsc(scheduleObjectId);

    // 모든 댓글 조회 (답글 포함)
    List<Comment> allComments = commentRepository.findByScheduleIdOrderByCreatedAtAsc(scheduleObjectId);

    // 작성자 및 멘션된 사용자 정보를 한 번에 조회
    List<ObjectId> allUserIds = new ArrayList<>();
    allComments.forEach(comment -> {
      allUserIds.add(comment.getAuthorId());
      if (comment.getMentions() != null) {
        allUserIds.addAll(comment.getMentions());
      }
    });

    Map<ObjectId, User> userMap = userRepository.findAllById(allUserIds.stream().distinct().collect(Collectors.toList()))
        .stream().collect(Collectors.toMap(User::getId, user -> user));

    // 현재 사용자 조회
    User currentUser = userRepository.findByEmail(email).orElse(null);
    ObjectId currentUserId = currentUser != null ? currentUser.getId() : null;

    // 댓글별 리액션 조회
    List<ObjectId> commentIds = allComments.stream().map(Comment::getId).collect(Collectors.toList());
    Map<ObjectId, List<CommentReaction>> reactionsByComment = commentReactionRepository.findAll().stream()
        .filter(r -> commentIds.contains(r.getCommentId()))
        .collect(Collectors.groupingBy(CommentReaction::getCommentId));

    // 답글 매핑
    Map<ObjectId, List<Comment>> repliesByParent = allComments.stream()
        .filter(c -> c.getParentId() != null)
        .collect(Collectors.groupingBy(Comment::getParentId));

    return parentComments.stream()
        .map(comment -> buildCommentResponse(comment, userMap, repliesByParent, reactionsByComment, currentUserId))
        .collect(Collectors.toList());
  }

  // 댓글 목록 조회 (기존 호환용)
  @Transactional(readOnly = true)
  public List<CommentResponseDto> getCommentsByScheduleId(String scheduleId) {
    return getCommentsByScheduleId(scheduleId, null);
  }

  private CommentResponseDto buildCommentResponse(
      Comment comment,
      Map<ObjectId, User> userMap,
      Map<ObjectId, List<Comment>> repliesByParent,
      Map<ObjectId, List<CommentReaction>> reactionsByComment,
      ObjectId currentUserId) {

    User author = userMap.get(comment.getAuthorId());
    CommentResponseDto dto = CommentResponseDto.from(comment,
        author != null ? author.getName() : "알 수 없음",
        author != null ? author.getAvatarUrl() : null);

    // 멘션된 사용자 정보 설정
    if (comment.getMentions() != null && !comment.getMentions().isEmpty()) {
      List<MentionedUserDto> mentions = comment.getMentions().stream()
          .map(mentionId -> {
            User mentionedUser = userMap.get(mentionId);
            return MentionedUserDto.builder()
                .userId(mentionId.toHexString())
                .userName(mentionedUser != null ? mentionedUser.getName() : "알 수 없음")
                .build();
          })
          .collect(Collectors.toList());
      dto.setMentions(mentions);
    }

    // 리액션 정보 설정
    List<CommentReaction> reactions = reactionsByComment.getOrDefault(comment.getId(), List.of());
    if (!reactions.isEmpty()) {
      Map<String, List<CommentReaction>> groupedByEmoji = reactions.stream()
          .collect(Collectors.groupingBy(CommentReaction::getEmoji));

      List<ReactionResponseDto> reactionDtos = groupedByEmoji.entrySet().stream()
          .map(entry -> ReactionResponseDto.builder()
              .emoji(entry.getKey())
              .count(entry.getValue().size())
              .isReactedByMe(entry.getValue().stream()
                  .anyMatch(r -> r.getUserId().equals(currentUserId)))
              .build())
          .collect(Collectors.toList());
      dto.setReactions(reactionDtos);
    }

    // 답글 설정
    List<Comment> replies = repliesByParent.getOrDefault(comment.getId(), List.of());
    if (!replies.isEmpty()) {
      List<CommentResponseDto> replyDtos = replies.stream()
          .map(reply -> buildCommentResponse(reply, userMap, Map.of(), reactionsByComment, currentUserId))
          .collect(Collectors.toList());
      dto.setReplies(replyDtos);
    }

    return dto;
  }

  // 댓글 작성
  @Transactional
  public CommentResponseDto createComment(String scheduleId, CreateCommentDto dto, String email) {
    User author = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

    // 멘션된 사용자 ID 변환
    List<ObjectId> mentionIds = new ArrayList<>();
    if (dto.getMentions() != null && !dto.getMentions().isEmpty()) {
      mentionIds = dto.getMentions().stream()
          .map(ObjectId::new)
          .collect(Collectors.toList());
    }

    // 부모 댓글 ID 변환
    ObjectId parentId = null;
    if (dto.getParentId() != null && !dto.getParentId().isEmpty()) {
      parentId = new ObjectId(dto.getParentId());
      // 부모 댓글 존재 확인
      commentRepository.findById(parentId)
          .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다"));
    }

    Comment comment = Comment.builder()
        .scheduleId(new ObjectId(scheduleId))
        .content(dto.getContent())
        .authorId(author.getId())
        .parentId(parentId)
        .mentions(mentionIds)
        .isEdited(false)
        .build();

    Comment saved = commentRepository.save(comment);

    // 알림 발송
    sendCommentNotifications(saved, author, scheduleId, parentId);

    return CommentResponseDto.from(saved, author.getName(), author.getAvatarUrl());
  }

  // 댓글 알림 발송
  private void sendCommentNotifications(Comment comment, User author, String scheduleId, ObjectId parentId) {
    try {
      Schedule schedule = scheduleRepository.findById(new ObjectId(scheduleId)).orElse(null);
      if (schedule == null) return;

      String scheduleTitle = schedule.getTitle();

      // 1. 멘션 알림
      if (comment.getMentions() != null && !comment.getMentions().isEmpty()) {
        for (ObjectId mentionedUserId : comment.getMentions()) {
          if (!mentionedUserId.equals(author.getId())) {
            User mentionedUser = userRepository.findById(mentionedUserId).orElse(null);
            if (mentionedUser != null && mentionedUser.getFcmToken() != null) {
              notificationService.sendPushNotification(
                  mentionedUser,
                  "[" + scheduleTitle + "] " + author.getName() + "님이 회원님을 언급했습니다",
                  comment.getContent()
              );
            }
          }
        }
      }

      // 2. 답글 알림 (원댓글 작성자에게)
      if (parentId != null) {
        Comment parentComment = commentRepository.findById(parentId).orElse(null);
        if (parentComment != null && !parentComment.getAuthorId().equals(author.getId())) {
          User parentAuthor = userRepository.findById(parentComment.getAuthorId()).orElse(null);
          if (parentAuthor != null && parentAuthor.getFcmToken() != null) {
            notificationService.sendPushNotification(
                parentAuthor,
                "[" + scheduleTitle + "] " + author.getName() + "님이 답글을 남겼습니다",
                comment.getContent()
            );
          }
        }
      }

      // 3. 일정 생성자에게 새 댓글 알림 (답글이 아닌 경우)
      if (parentId == null && schedule.getParticipants() != null && !schedule.getParticipants().isEmpty()) {
        ObjectId creatorId = schedule.getParticipants().get(0); // 첫 번째 참여자를 생성자로 간주
        if (!creatorId.equals(author.getId())) {
          User creator = userRepository.findById(creatorId).orElse(null);
          if (creator != null && creator.getFcmToken() != null) {
            notificationService.sendPushNotification(
                creator,
                "[" + scheduleTitle + "] 새 댓글이 달렸습니다",
                author.getName() + ": " + comment.getContent()
            );
          }
        }
      }
    } catch (Exception e) {
      // 알림 실패해도 댓글 작성은 성공으로 처리
    }
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
    comment.setIsEdited(true);  // 수정됨 표시
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

    // 답글도 함께 삭제
    List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(comment.getId());
    commentRepository.deleteAll(replies);

    commentRepository.delete(comment);
  }

  // 댓글 수 조회
  @Transactional(readOnly = true)
  public long getCommentCount(String scheduleId) {
    return commentRepository.countByScheduleId(new ObjectId(scheduleId));
  }
}
