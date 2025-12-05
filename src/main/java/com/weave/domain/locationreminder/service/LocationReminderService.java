package com.weave.domain.locationreminder.service;

import com.weave.domain.locationreminder.dto.ArrivalNotificationDto;
import com.weave.domain.locationreminder.dto.LocationReminderDto;
import com.weave.domain.locationreminder.dto.SetLocationReminderDto;
import com.weave.domain.locationreminder.dto.ToggleLocationReminderDto;
import com.weave.domain.locationreminder.entity.LocationReminder;
import com.weave.domain.locationreminder.repository.LocationReminderRepository;
import com.weave.domain.schedule.entity.Schedule;
import com.weave.domain.schedule.repository.ScheduleRepository;
import com.weave.domain.schedule.service.NotificationService;
import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
import com.weave.global.exception.BusinessException;
import com.weave.global.exception.ErrorCode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationReminderService {

  private final LocationReminderRepository locationReminderRepository;
  private final UserRepository userRepository;
  private final ScheduleRepository scheduleRepository;
  private final NotificationService notificationService;

  private static final SimpleDateFormat dateFormat;

  static {
    dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  public LocationReminderDto getReminder(String scheduleId) {
    ObjectId scheduleOid = new ObjectId(scheduleId);
    Optional<LocationReminder> reminderOpt = locationReminderRepository.findByScheduleId(scheduleOid);
    return reminderOpt.map(this::toDto).orElse(null);
  }

  @Transactional
  public LocationReminderDto setReminder(String scheduleId, SetLocationReminderDto dto, String email) {
    ObjectId scheduleOid = new ObjectId(scheduleId);
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    Optional<LocationReminder> existingOpt = locationReminderRepository.findByScheduleId(scheduleOid);

    LocationReminder reminder;
    if (existingOpt.isPresent()) {
      reminder = existingOpt.get();
      reminder.setLatitude(dto.getLatitude());
      reminder.setLongitude(dto.getLongitude());
      reminder.setRadius(dto.getRadius() != null ? dto.getRadius() : 300);
      reminder.setAddress(dto.getAddress());
      reminder.setPlaceName(dto.getPlaceName());
      reminder.setUpdatedAt(new Date());
    } else {
      reminder = LocationReminder.builder()
          .scheduleId(scheduleOid)
          .latitude(dto.getLatitude())
          .longitude(dto.getLongitude())
          .radius(dto.getRadius() != null ? dto.getRadius() : 300)
          .address(dto.getAddress())
          .placeName(dto.getPlaceName())
          .isEnabled(true)
          .createdBy(user.getId())
          .createdAt(new Date())
          .build();
    }

    LocationReminder saved = locationReminderRepository.save(reminder);
    return toDto(saved);
  }

  @Transactional
  public LocationReminderDto toggleReminder(String scheduleId, ToggleLocationReminderDto dto) {
    ObjectId scheduleOid = new ObjectId(scheduleId);

    LocationReminder reminder = locationReminderRepository.findByScheduleId(scheduleOid)
        .orElseThrow(() -> new IllegalArgumentException("위치 알림 설정이 없습니다."));

    reminder.setIsEnabled(dto.getIsEnabled());
    reminder.setUpdatedAt(new Date());

    LocationReminder saved = locationReminderRepository.save(reminder);
    return toDto(saved);
  }

  @Transactional
  public void deleteReminder(String scheduleId) {
    ObjectId scheduleOid = new ObjectId(scheduleId);
    locationReminderRepository.deleteByScheduleId(scheduleOid);
  }

  private LocationReminderDto toDto(LocationReminder reminder) {
    return LocationReminderDto.builder()
        .id(reminder.getId().toHexString())
        .scheduleId(reminder.getScheduleId().toHexString())
        .latitude(reminder.getLatitude())
        .longitude(reminder.getLongitude())
        .radius(reminder.getRadius())
        .address(reminder.getAddress())
        .placeName(reminder.getPlaceName())
        .isEnabled(reminder.getIsEnabled())
        .triggeredAt(reminder.getTriggeredAt() != null ? dateFormat.format(reminder.getTriggeredAt()) : null)
        .build();
  }

  /**
   * 위치 도착 알림 처리
   * 사용자가 설정된 위치 반경 내에 들어왔을 때 호출
   */
  @Transactional
  public void notifyArrival(String scheduleId, ArrivalNotificationDto dto, String email) {
    ObjectId scheduleOid = new ObjectId(scheduleId);
    User arrivingUser = userRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    // 위치 알림 설정 조회
    LocationReminder reminder = locationReminderRepository.findByScheduleId(scheduleOid)
        .orElseThrow(() -> new IllegalArgumentException("위치 알림 설정이 없습니다."));

    // 비활성화된 알림은 무시
    if (!reminder.getIsEnabled()) {
      log.info("Location reminder is disabled for schedule {}", scheduleId);
      return;
    }

    // 거리 계산 (Haversine 공식)
    double distance = calculateDistance(
        dto.getLatitude(), dto.getLongitude(),
        reminder.getLatitude(), reminder.getLongitude()
    );

    // 반경 밖이면 무시
    if (distance > reminder.getRadius()) {
      log.info("User {} is outside the radius. Distance: {}m, Radius: {}m",
          arrivingUser.getName(), distance, reminder.getRadius());
      return;
    }

    // 스케줄 조회
    Schedule schedule = scheduleRepository.findById(scheduleOid)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    // 참여자들에게 알림 전송 (도착한 사용자 제외)
    List<ObjectId> participantIds = schedule.getParticipants();
    if (participantIds == null || participantIds.isEmpty()) {
      log.info("No participants in schedule to notify");
      return;
    }

    List<User> participants = userRepository.findAllById(participantIds);

    String placeName = reminder.getPlaceName() != null ? reminder.getPlaceName() : reminder.getAddress();
    String title = "위치 도착 알림";
    String body = String.format("%s님이 %s에 도착했습니다.", arrivingUser.getName(), placeName);

    int notifiedCount = 0;
    for (User participant : participants) {
      // 도착한 사용자 본인에게는 알림 전송 안함
      if (participant.getId().equals(arrivingUser.getId())) {
        continue;
      }

      notificationService.sendPushNotificationWithData(
          participant,
          title,
          body,
          Map.of(
              "type", "location_arrival",
              "scheduleId", scheduleId,
              "scheduleTitle", schedule.getTitle(),
              "arrivedUserId", arrivingUser.getId().toHexString(),
              "arrivedUserName", arrivingUser.getName(),
              "placeName", placeName != null ? placeName : ""
          )
      );
      notifiedCount++;
    }

    // 도착 시간 기록
    reminder.setTriggeredAt(new Date());
    locationReminderRepository.save(reminder);

    log.info("Sent arrival notification to {} participants for schedule {}", notifiedCount, scheduleId);
  }

  /**
   * Haversine 공식을 사용한 두 지점 간 거리 계산 (미터 단위)
   */
  private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
    final int R = 6371000; // 지구 반지름 (미터)

    double latDistance = Math.toRadians(lat2 - lat1);
    double lonDistance = Math.toRadians(lon2 - lon1);

    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return R * c;
  }
}
