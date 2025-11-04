package com.weave.domain.schedule.scheduler;

import com.weave.domain.schedule.entity.Schedule;
import com.weave.domain.schedule.repository.ScheduleRepository;
import com.weave.domain.schedule.service.NotificationService;
import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.bson.types.ObjectId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 매일 오전 9시에 오늘의 일정 알림 발송
     */
    @Scheduled(cron = "0 0 9 * * *")
    @SchedulerLock(name = "sendDailyScheduleNotification", 
            lockAtMostFor = "10m", 
            lockAtLeastFor = "1m")
    public void sendDailyScheduleNotification() {
        log.info("Starting daily schedule notification batch");

        LocalDate today = LocalDate.now();
        String todayStr = today.atStartOfDay().format(DATE_FORMATTER);

        // 오늘 시작하는 일정 조회
        List<Schedule> todaySchedules = scheduleRepository.findSchedulesStartingToday(todayStr);
        
        log.info("Found {} schedules for today", todaySchedules.size());

        for (Schedule schedule : todaySchedules) {
            sendScheduleNotificationToParticipants(schedule);
        }

        log.info("Completed daily schedule notification batch");
    }

    /**
     * 매일 오전 9시에 기념일 알림 발송
     */
    @Scheduled(cron = "0 0 9 * * *")
    @SchedulerLock(name = "sendAnniversaryNotification", 
            lockAtMostFor = "10m", 
            lockAtLeastFor = "1m")
    public void sendAnniversaryNotification() {
        log.info("Starting anniversary notification batch");

        LocalDate today = LocalDate.now();
        String todayStr = today.atStartOfDay().format(DATE_FORMATTER);

        // 오늘이 기념일인 일정 조회
        List<Schedule> anniversaries = scheduleRepository.findAnniversariesToday(todayStr);
        
        log.info("Found {} anniversaries for today", anniversaries.size());

        for (Schedule anniversary : anniversaries) {
            sendAnniversaryNotificationToParticipants(anniversary);
        }

        log.info("Completed anniversary notification batch");
    }

    /**
     * 1시간마다 곧 시작할 일정 알림 발송 (1시간 전 알림)
     */
    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "sendUpcomingScheduleNotification", 
            lockAtMostFor = "10m", 
            lockAtLeastFor = "1m")
    public void sendUpcomingScheduleNotification() {
        log.info("Starting upcoming schedule notification batch");

        LocalDateTime oneHourLater = LocalDateTime.now().plusHours(1);
        String oneHourLaterStr = oneHourLater.format(DATE_FORMATTER);

        // 1시간 후 시작하는 일정 조회
        List<Schedule> upcomingSchedules = scheduleRepository.findSchedulesStartingAt(oneHourLaterStr);
        
        log.info("Found {} upcoming schedules", upcomingSchedules.size());

        for (Schedule schedule : upcomingSchedules) {
            sendUpcomingNotificationToParticipants(schedule);
        }

        log.info("Completed upcoming schedule notification batch");
    }

    private void sendScheduleNotificationToParticipants(Schedule schedule) {
        List<User> participants = getUsersByIds(schedule.getParticipants());

        String title = "오늘의 일정";
        String body = String.format("'%s' 일정이 오늘 시작됩니다.", schedule.getTitle());

        for (User user : participants) {
            if (user.isScheduleAlarm()) {
                notificationService.sendPushNotification(user, title, body);
            }
        }
    }

    private void sendAnniversaryNotificationToParticipants(Schedule schedule) {
        List<User> participants = getUsersByIds(schedule.getParticipants());

        String title = "기념일 알림";
        String body = String.format("오늘은 '%s' 기념일입니다! 🎉", schedule.getTitle());

        for (User user : participants) {
            if (user.isAnniversaryAlarm()) {
                notificationService.sendPushNotification(user, title, body);
            }
        }
    }

    private void sendUpcomingNotificationToParticipants(Schedule schedule) {
        List<User> participants = getUsersByIds(schedule.getParticipants());

        String title = "일정 알림";
        String body = String.format("'%s' 일정이 1시간 후 시작됩니다.", schedule.getTitle());

        for (User user : participants) {
            if (user.isScheduleAlarm()) {
                notificationService.sendPushNotification(user, title, body);
            }
        }
    }

    private List<User> getUsersByIds(List<ObjectId> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        return userRepository.findAllById(userIds);
    }
}
