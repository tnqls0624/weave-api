package com.weave.domain.reaction.service;

import com.weave.domain.comment.entity.Comment;
import com.weave.domain.comment.repository.CommentRepository;
import com.weave.domain.reaction.dto.ReactionResponseDto;
import com.weave.domain.reaction.dto.ReactionResponseDto.ReactedUserDto;
import com.weave.domain.reaction.dto.ReactionSummaryDto;
import com.weave.domain.reaction.entity.CommentReaction;
import com.weave.domain.reaction.entity.ScheduleReaction;
import com.weave.domain.reaction.repository.CommentReactionRepository;
import com.weave.domain.reaction.repository.ScheduleReactionRepository;
import com.weave.domain.schedule.service.NotificationService;
import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactionService {

  private final ScheduleReactionRepository scheduleReactionRepository;
  private final CommentReactionRepository commentReactionRepository;
  private final CommentRepository commentRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;

  // 허용된 이모지 목록
  private static final List<String> ALLOWED_EMOJIS = Arrays.asList("👍", "❤️", "🎉", "👀", "🙏", "😢");

  // ===== 일정 리액션 =====

  @Transactional
  public ReactionSummaryDto toggleScheduleReaction(String scheduleId, String emoji, String email) {
    if (!ALLOWED_EMOJIS.contains(emoji)) {
      throw new IllegalArgumentException("허용되지 않은 이모지입니다");
    }

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

    ObjectId scheduleObjectId = new ObjectId(scheduleId);

    Optional<ScheduleReaction> existing = scheduleReactionRepository
        .findByScheduleIdAndUserIdAndEmoji(scheduleObjectId, user.getId(), emoji);

    if (existing.isPresent()) {
      // 이미 있으면 삭제 (토글 off)
      scheduleReactionRepository.delete(existing.get());
    } else {
      // 없으면 추가 (토글 on)
      ScheduleReaction reaction = ScheduleReaction.builder()
          .scheduleId(scheduleObjectId)
          .userId(user.getId())
          .emoji(emoji)
          .build();
      scheduleReactionRepository.save(reaction);
    }

    return getScheduleReactions(scheduleId, email);
  }

  @Transactional(readOnly = true)
  public ReactionSummaryDto getScheduleReactions(String scheduleId, String email) {
    log.debug("Getting reactions for schedule: {}, user: {}", scheduleId, email);

    try {
      ObjectId scheduleObjectId = new ObjectId(scheduleId);
      List<ScheduleReaction> allReactions = scheduleReactionRepository.findByScheduleId(scheduleObjectId);
      log.debug("Found {} reactions for schedule {}", allReactions.size(), scheduleId);

      User currentUser = userRepository.findByEmail(email).orElse(null);
      ObjectId currentUserId = currentUser != null ? currentUser.getId() : null;

      // 사용자 정보 조회
      List<ObjectId> userIds = allReactions.stream()
          .map(ScheduleReaction::getUserId)
          .distinct()
          .collect(Collectors.toList());
      Map<ObjectId, User> userMap = userRepository.findAllById(userIds).stream()
          .collect(Collectors.toMap(User::getId, u -> u));

      // 이모지별로 그룹화
      Map<String, List<ScheduleReaction>> groupedByEmoji = allReactions.stream()
          .collect(Collectors.groupingBy(ScheduleReaction::getEmoji));

      List<ReactionResponseDto> reactions = new ArrayList<>();
      for (String emoji : ALLOWED_EMOJIS) {
        List<ScheduleReaction> emojiReactions = groupedByEmoji.getOrDefault(emoji, List.of());
        if (!emojiReactions.isEmpty()) {
          boolean isReactedByMe = emojiReactions.stream()
              .anyMatch(r -> r.getUserId().equals(currentUserId));

          List<ReactedUserDto> users = emojiReactions.stream()
              .map(r -> {
                User user = userMap.get(r.getUserId());
                return ReactedUserDto.builder()
                    .userId(r.getUserId().toHexString())
                    .userName(user != null ? user.getName() : "알 수 없음")
                    .avatarUrl(user != null ? user.getAvatarUrl() : null)
                    .build();
              })
              .collect(Collectors.toList());

          reactions.add(ReactionResponseDto.builder()
              .emoji(emoji)
              .count(emojiReactions.size())
              .isReactedByMe(isReactedByMe)
              .users(users)
              .build());
        }
      }

      return ReactionSummaryDto.builder()
          .reactions(reactions)
          .build();
    } catch (IllegalArgumentException e) {
      log.error("Invalid scheduleId format: {}", scheduleId, e);
      throw e;
    } catch (Exception e) {
      log.error("Error getting schedule reactions for scheduleId: {}, email: {}", scheduleId, email, e);
      throw new RuntimeException("Failed to get schedule reactions", e);
    }
  }

  // ===== 댓글 리액션 =====

  @Transactional
  public long toggleCommentReaction(String commentId, String emoji, String email) {
    User reactor = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

    ObjectId commentObjectId = new ObjectId(commentId);

    Optional<CommentReaction> existing = commentReactionRepository
        .findByCommentIdAndUserIdAndEmoji(commentObjectId, reactor.getId(), emoji);

    if (existing.isPresent()) {
      // 리액션 제거 (토글 off) - 알림 안 보냄
      commentReactionRepository.delete(existing.get());
    } else {
      // 리액션 추가 (토글 on) - 알림 보냄
      CommentReaction reaction = CommentReaction.builder()
          .commentId(commentObjectId)
          .userId(reactor.getId())
          .emoji(emoji)
          .build();
      commentReactionRepository.save(reaction);

      // 댓글 작성자에게 FCM 알림 전송
      sendReactionNotification(commentObjectId, reactor, emoji);
    }

    return commentReactionRepository.countByCommentIdAndEmoji(commentObjectId, emoji);
  }

  /**
   * 댓글 리액션 알림 전송
   */
  private void sendReactionNotification(ObjectId commentId, User reactor, String emoji) {
    try {
      Comment comment = commentRepository.findById(commentId).orElse(null);
      if (comment == null) {
        log.warn("Comment not found for reaction notification: {}", commentId);
        return;
      }

      // 댓글 작성자 조회
      User commentAuthor = userRepository.findById(comment.getAuthorId()).orElse(null);
      if (commentAuthor == null) {
        log.warn("Comment author not found: {}", comment.getAuthorId());
        return;
      }

      // 자기 자신의 댓글에 리액션한 경우 알림 안 보냄
      if (commentAuthor.getId().equals(reactor.getId())) {
        return;
      }

      String title = "댓글에 반응이 달렸어요";
      String body = String.format("%s님이 회원님의 댓글에 %s 반응을 남겼어요", reactor.getName(), emoji);

      notificationService.sendPushNotification(commentAuthor, title, body);
      log.info("Sent reaction notification to user {} for comment {}", commentAuthor.getId(), commentId);
    } catch (Exception e) {
      log.error("Failed to send reaction notification for comment {}: {}", commentId, e.getMessage());
    }
  }

  @Transactional(readOnly = true)
  public List<ReactionResponseDto> getCommentReactions(String commentId, String email) {
    ObjectId commentObjectId = new ObjectId(commentId);
    List<CommentReaction> allReactions = commentReactionRepository.findByCommentId(commentObjectId);

    User currentUser = userRepository.findByEmail(email).orElse(null);
    ObjectId currentUserId = currentUser != null ? currentUser.getId() : null;

    // 사용자 정보 조회
    List<ObjectId> userIds = allReactions.stream()
        .map(CommentReaction::getUserId)
        .distinct()
        .collect(Collectors.toList());
    Map<ObjectId, User> userMap = userRepository.findAllById(userIds).stream()
        .collect(Collectors.toMap(User::getId, u -> u));

    // 이모지별로 그룹화 (댓글은 👍만 사용)
    Map<String, List<CommentReaction>> groupedByEmoji = allReactions.stream()
        .collect(Collectors.groupingBy(CommentReaction::getEmoji));

    List<ReactionResponseDto> reactions = new ArrayList<>();
    for (Map.Entry<String, List<CommentReaction>> entry : groupedByEmoji.entrySet()) {
      String emoji = entry.getKey();
      List<CommentReaction> emojiReactions = entry.getValue();

      boolean isReactedByMe = emojiReactions.stream()
          .anyMatch(r -> r.getUserId().equals(currentUserId));

      List<ReactedUserDto> users = emojiReactions.stream()
          .map(r -> {
            User user = userMap.get(r.getUserId());
            return ReactedUserDto.builder()
                .userId(r.getUserId().toHexString())
                .userName(user != null ? user.getName() : "알 수 없음")
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .build();
          })
          .collect(Collectors.toList());

      reactions.add(ReactionResponseDto.builder()
          .emoji(emoji)
          .count(emojiReactions.size())
          .isReactedByMe(isReactedByMe)
          .users(users)
          .build());
    }

    return reactions;
  }
}
