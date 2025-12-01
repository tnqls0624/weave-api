package com.weave.domain.schedule.scheduler;

import com.weave.domain.schedule.entity.Schedule;
import com.weave.domain.schedule.repository.ScheduleRepository;
import com.weave.domain.schedule.service.NotificationService;
import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

  /**
   * 매일 오전 6시에 오늘의 일정 알림 발송 - 모든 워크스페이스의 오늘 일정 조회 - 각 참여자에게 알림 발송
   */
  @Scheduled(cron = "0 0 6 * * *")
  @SchedulerLock(name = "sendDailyScheduleNotification",
      lockAtMostFor = "10m",
      lockAtLeastFor = "1m")
  public void sendDailyScheduleNotification() {
    log.info("Starting daily schedule notification batch");

    LocalDate today = LocalDate.now();
    Date startOfDay = Date.from(today.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant());

    Calendar cal = Calendar.getInstance();
    cal.setTime(startOfDay);
    cal.add(Calendar.DAY_OF_MONTH, 1);
    Date endOfDay = cal.getTime();

    // 오늘 시작하는 모든 일정 조회 (모든 워크스페이스)
    List<Schedule> todaySchedules = scheduleRepository.findSchedulesStartingToday(startOfDay,
        endOfDay);

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
      if (user.getPushEnabled()) {
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
      body = String.format("'%s' 일정이 오늘 있습니다.", schedules.getFirst().getTitle());
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

  /**
   * 매분 실행되는 일정 리마인더 알림 발송 - reminderMinutes가 설정된 일정 중, 시작 시간까지 reminderMinutes 남은 일정을 찾아 알림 발송 - 예:
   * reminderMinutes가 30이면, 일정 시작 30분 전에 알림 발송
   */
  @Scheduled(cron = "0 * * * * *")  // 매분 0초에 실행
  @SchedulerLock(name = "sendScheduleReminderNotification",
      lockAtMostFor = "1m",
      lockAtLeastFor = "30s")
  public void sendScheduleReminderNotification() {
    log.debug("Starting schedule reminder notification check");

    ZoneId seoulZone = ZoneId.of("Asia/Seoul");
    LocalDateTime now = LocalDateTime.now(seoulZone);

    // 현재 시간부터 최대 알림 시간(24시간 = 1440분) 이후까지의 일정 조회
    // 더 효율적으로 하기 위해 실제로는 현재 + 최대 reminderMinutes 범위만 조회
    Date searchStart = Date.from(now.atZone(seoulZone).toInstant());
    Date searchEnd = Date.from(now.plusHours(24).atZone(seoulZone).toInstant());

    List<Schedule> schedulesWithReminder = scheduleRepository.findSchedulesForReminder(searchStart,
        searchEnd);

    if (schedulesWithReminder.isEmpty()) {
      log.debug("No schedules with reminder found");
      return;
    }

    log.info("Found {} schedules with reminder settings", schedulesWithReminder.size());

    for (Schedule schedule : schedulesWithReminder) {
      if (schedule.getReminderMinutes() == null || schedule.getStartDate() == null) {
        continue;
      }

      // 일정 시작 시간
      Instant scheduleStart = schedule.getStartDate().toInstant();
      LocalDateTime scheduleStartTime = LocalDateTime.ofInstant(scheduleStart, seoulZone);

      // 알림 발송 시간 계산 (일정 시작 - reminderMinutes)
      LocalDateTime reminderTime = scheduleStartTime.minusMinutes(schedule.getReminderMinutes());

      // 현재 시간이 알림 발송 시간과 같은 분인지 확인 (초는 무시)
      LocalDateTime nowTruncated = now.truncatedTo(ChronoUnit.MINUTES);
      LocalDateTime reminderTruncated = reminderTime.truncatedTo(ChronoUnit.MINUTES);

      if (nowTruncated.equals(reminderTruncated)) {
        log.info("Sending reminder for schedule: {} (starts at {})", schedule.getTitle(),
            scheduleStartTime);
        sendReminderNotification(schedule);

        // 알림 발송 완료 표시
        schedule.setReminderSent(true);
        scheduleRepository.save(schedule);
      }
    }

    log.debug("Completed schedule reminder notification check");
  }

  /**
   * 리마인더 알림 발송
   */
  private void sendReminderNotification(Schedule schedule) {
    if (schedule.getParticipants() == null || schedule.getParticipants().isEmpty()) {
      return;
    }

    String title = "일정 알림";
    String body;
    int minutes = schedule.getReminderMinutes();

    if (minutes == 0) {
      body = String.format("'%s' 일정이 지금 시작됩니다.", schedule.getTitle());
    } else if (minutes >= 1440) {
      int days = minutes / 1440;
      body = String.format("'%s' 일정이 %d일 후에 시작됩니다.", schedule.getTitle(), days);
    } else if (minutes >= 60) {
      int hours = minutes / 60;
      body = String.format("'%s' 일정이 %d시간 후에 시작됩니다.", schedule.getTitle(), hours);
    } else {
      body = String.format("'%s' 일정이 %d분 후에 시작됩니다.", schedule.getTitle(), minutes);
    }

    for (ObjectId userId : schedule.getParticipants()) {
      User user = userRepository.findById(userId).orElse(null);
      if (user == null) {
        log.warn("User not found for reminder: {}", userId);
        continue;
      }

      if (user.getPushEnabled()) {
        notificationService.sendPushNotification(user, title, body);
        log.info("Sent reminder to user {} for schedule {}", user.getId(), schedule.getTitle());
      }
    }
  }

}
