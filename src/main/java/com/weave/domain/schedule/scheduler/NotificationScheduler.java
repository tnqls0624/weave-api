package com.weave.domain.schedule.scheduler;

import com.weave.domain.schedule.entity.Schedule;
import com.weave.domain.schedule.repository.ScheduleRepository;
import com.weave.domain.schedule.service.NotificationService;
import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.bson.types.ObjectId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

  private final ScheduleRepository scheduleRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
      "yyyy-MM-dd HH:mm:ss");

  /**
   * 매일 오전 9시에 오늘의 일정 알림 발송 - 모든 워크스페이스의 오늘 일정 조회 - 각 참여자에게 알림 발송
   */
  @Scheduled(cron = "0 0 9 * * *")
  @SchedulerLock(name = "sendDailyScheduleNotification",
      lockAtMostFor = "10m",
      lockAtLeastFor = "1m")
  public void sendDailyScheduleNotification() {
    log.info("Starting daily schedule notification batch");

    LocalDate today = LocalDate.now();
    String todayStr = today.atStartOfDay().format(DATE_FORMATTER);

    // 오늘 시작하는 모든 일정 조회 (모든 워크스페이스)
    List<Schedule> todaySchedules = scheduleRepository.findSchedulesStartingToday(todayStr);

    log.info("Found {} schedules for today", todaySchedules.size());

    // 유저별로 일정을 그룹화하여 알림 발송
    Map<ObjectId, List<Schedule>> schedulesByUser = groupSchedulesByUser(todaySchedules);

    log.info("Sending notifications to {} users", schedulesByUser.size());

    for (Map.Entry<ObjectId, List<Schedule>> entry : schedulesByUser.entrySet()) {
      ObjectId userId = entry.getKey();
      List<Schedule> userSchedules = entry.getValue();

      User user = userRepository.findById(userId).orElse(null);
      if (user == null) {
        log.warn("User not found: {}", userId);
        continue;
      }

      // 일정 알림 설정이 켜져있는 유저에게만 알림 발송
      if (user.isScheduleAlarm()) {
        sendScheduleNotification(user, userSchedules);
      }
    }

    log.info("Completed daily schedule notification batch");
  }

  /**
   * 일정을 참여자별로 그룹화
   */
  private Map<ObjectId, List<Schedule>> groupSchedulesByUser(List<Schedule> schedules) {
    Map<ObjectId, List<Schedule>> schedulesByUser = new HashMap<>();

    for (Schedule schedule : schedules) {
      if (schedule.getParticipants() == null || schedule.getParticipants().isEmpty()) {
        continue;
      }

      for (ObjectId userId : schedule.getParticipants()) {
        schedulesByUser.computeIfAbsent(userId, k -> new ArrayList<>()).add(schedule);
      }
    }

    return schedulesByUser;
  }


  /**
   * 일반 일정 알림 발송
   */
  private void sendScheduleNotification(User user, List<Schedule> schedules) {
    String title = "오늘의 일정";
    String body;

    if (schedules.size() == 1) {
      body = String.format("'%s' 일정이 오늘 있습니다.", schedules.get(0).getTitle());
    } else {
      String scheduleTitles = schedules.stream()
          .limit(3)
          .map(Schedule::getTitle)
          .collect(Collectors.joining(", "));

      if (schedules.size() > 3) {
        body = String.format("'%s' 외 %d개의 일정이 오늘 있습니다.",
            scheduleTitles, schedules.size() - 3);
      } else {
        body = String.format("'%s' 일정이 오늘 있습니다.", scheduleTitles);
      }
    }

    notificationService.sendPushNotification(user, title, body);
  }

}
