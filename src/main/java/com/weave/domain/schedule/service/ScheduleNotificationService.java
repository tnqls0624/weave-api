package com.weave.domain.schedule.service;

import com.weave.domain.schedule.entity.Schedule;
import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
import com.weave.domain.workspace.entity.Workspace;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class ScheduleNotificationService {

  private final UserRepository userRepository;
  private final NotificationService notificationService;

  /**
   * 일정 생성 시 워크스페이스 참여자들에게 알림 발송
   */
  @Async
  public void sendScheduleCreatedNotification(Workspace workspace, Schedule schedule) {
    if (workspace.getUsers() == null || workspace.getUsers().isEmpty()) {
      log.info("No users in workspace to notify");
      return;
    }

    // 워크스페이스의 모든 참여자 조회
    List<User> participants = userRepository.findAllById(workspace.getUsers());

    String title = "새로운 일정";
    String body = String.format("'%s' 일정이 등록되었습니다.", schedule.getTitle());

    Map<String, String> data = new HashMap<>();
    data.put("type", "SCHEDULE_CREATE");
    data.put("scheduleId", schedule.getId().toHexString());
    data.put("scheduleTitle", schedule.getTitle());

    participants.parallelStream()
        .filter(User::getPushEnabled)
        .forEach(user -> notificationService.sendPushNotificationWithData(user, title, body, data));

    log.info("Sent schedule creation notification to {} users", participants.size());
  }

  /**
   * 일정 수정 시 참여자들에게 알림 발송 (새로 추가된 참여자에게는 초대 알림)
   */
  @Async
  public void sendScheduleUpdatedNotification(Schedule schedule, List<ObjectId> oldParticipants) {
    if (schedule.getParticipants() == null || schedule.getParticipants().isEmpty()) {
      log.info("No participants in schedule to notify");
      return;
    }

    Set<ObjectId> oldParticipantSet = oldParticipants != null ? new HashSet<>(oldParticipants) : new HashSet<>();
    Set<ObjectId> newParticipantSet = new HashSet<>(schedule.getParticipants());

    // 새로 추가된 참여자 (초대 알림 대상)
    Set<ObjectId> newlyAddedParticipants = new HashSet<>(newParticipantSet);
    newlyAddedParticipants.removeAll(oldParticipantSet);

    // 기존 참여자 (수정 알림 대상)
    Set<ObjectId> existingParticipants = new HashSet<>(newParticipantSet);
    existingParticipants.retainAll(oldParticipantSet);

    // 새로 추가된 참여자에게 초대 알림 발송
    if (!newlyAddedParticipants.isEmpty()) {
      List<User> newUsers = userRepository.findAllById(newlyAddedParticipants);

      String inviteTitle = "일정 초대";
      String inviteBody = String.format("'%s' 일정에 초대되었습니다.", schedule.getTitle());

      Map<String, String> inviteData = new HashMap<>();
      inviteData.put("type", "SCHEDULE_INVITE");
      inviteData.put("scheduleId", schedule.getId().toHexString());
      inviteData.put("scheduleTitle", schedule.getTitle());

      newUsers.parallelStream()
          .filter(User::getPushEnabled)
          .forEach(user -> notificationService.sendPushNotificationWithData(user, inviteTitle, inviteBody, inviteData));

      log.info("Sent schedule invite notification to {} new participants", newUsers.size());
    }

    // 기존 참여자에게 수정 알림 발송
    if (!existingParticipants.isEmpty()) {
      List<User> existingUsers = userRepository.findAllById(existingParticipants);

      String updateTitle = "일정 변경";
      String updateBody = String.format("'%s' 일정이 수정되었습니다.", schedule.getTitle());

      Map<String, String> updateData = new HashMap<>();
      updateData.put("type", "SCHEDULE_UPDATE");
      updateData.put("scheduleId", schedule.getId().toHexString());
      updateData.put("scheduleTitle", schedule.getTitle());

      existingUsers.parallelStream()
          .filter(User::getPushEnabled)
          .forEach(user -> notificationService.sendPushNotificationWithData(user, updateTitle, updateBody, updateData));

      log.info("Sent schedule update notification to {} existing participants", existingUsers.size());
    }
  }

  /**
   * 일정 수정 시 참여자들에게 알림 발송 (기존 메서드 - 하위 호환성 유지)
   */
  @Async
  public void sendScheduleUpdatedNotification(Schedule schedule) {
    sendScheduleUpdatedNotification(schedule, null);
  }

  /**
   * 일정 삭제 시 참여자들에게 알림 발송
   */
  @Async
  public void sendScheduleDeletedNotification(Schedule schedule) {
    if (schedule.getParticipants() == null || schedule.getParticipants().isEmpty()) {
      log.info("No participants in schedule to notify");
      return;
    }

    List<User> participants = userRepository.findAllById(schedule.getParticipants());

    String title = "일정 삭제";
    String body = String.format("'%s' 일정이 삭제되었습니다.", schedule.getTitle());

    Map<String, String> data = new HashMap<>();
    data.put("type", "SCHEDULE_DELETE");
    data.put("scheduleId", schedule.getId().toHexString());
    data.put("scheduleTitle", schedule.getTitle());

    participants.parallelStream()
        .filter(User::getPushEnabled)
        .forEach(user -> notificationService.sendPushNotificationWithData(user, title, body, data));

    log.info("Sent schedule delete notification to {} users", participants.size());
  }

  /**
   * 일정 참여자 초대 알림 발송 (생성 시 참여자에게)
   */
  @Async
  public void sendScheduleInviteNotification(Schedule schedule, User inviter) {
    if (schedule.getParticipants() == null || schedule.getParticipants().isEmpty()) {
      log.info("No participants in schedule to invite");
      return;
    }

    List<User> participants = userRepository.findAllById(schedule.getParticipants());

    String title = "일정 초대";
    String inviterName = inviter != null ? inviter.getName() : "누군가";
    String body = String.format("%s님이 '%s' 일정에 초대했습니다.", inviterName, schedule.getTitle());

    Map<String, String> data = new HashMap<>();
    data.put("type", "SCHEDULE_INVITE");
    data.put("scheduleId", schedule.getId().toHexString());
    data.put("scheduleTitle", schedule.getTitle());
    data.put("inviterName", inviterName);

    participants.parallelStream()
        .filter(User::getPushEnabled)
        .filter(user -> inviter == null || !user.getId().equals(inviter.getId())) // 초대자 본인은 제외
        .forEach(user -> notificationService.sendPushNotificationWithData(user, title, body, data));

    log.info("Sent schedule invite notification from {} to {} participants", inviterName, participants.size());
  }
}
