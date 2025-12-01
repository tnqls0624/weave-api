package com.weave.domain.schedule.service;

import com.weave.domain.schedule.entity.Schedule;
import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
import com.weave.domain.workspace.entity.Workspace;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    participants.parallelStream()
        .filter(User::getPushEnabled)
        .forEach(user -> notificationService.sendPushNotification(user, title, body));

    log.info("Sent schedule creation notification to {} users", participants.size());
  }

  /**
   * 일정 수정 시 참여자들에게 알림 발송
   */
  @Async
  public void sendScheduleUpdatedNotification(Schedule schedule) {
    if (schedule.getParticipants() == null || schedule.getParticipants().isEmpty()) {
      log.info("No participants in schedule to notify");
      return;
    }

    List<User> participants = userRepository.findAllById(schedule.getParticipants());

    String title = "일정 변경";
    String body = String.format("'%s' 일정이 수정되었습니다.", schedule.getTitle());

    participants.parallelStream()
        .filter(User::getPushEnabled)
        .forEach(user -> notificationService.sendPushNotification(user, title, body));

    log.info("Sent schedule update notification to {} users", participants.size());
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

    participants.parallelStream()
        .filter(User::getPushEnabled)
        .forEach(user -> notificationService.sendPushNotification(user, title, body));

    log.info("Sent schedule delete notification to {} users", participants.size());
  }
}
