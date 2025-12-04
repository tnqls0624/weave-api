package com.weave.domain.locationreminder.service;

import com.weave.domain.locationreminder.dto.LocationReminderDto;
import com.weave.domain.locationreminder.dto.SetLocationReminderDto;
import com.weave.domain.locationreminder.dto.ToggleLocationReminderDto;
import com.weave.domain.locationreminder.entity.LocationReminder;
import com.weave.domain.locationreminder.repository.LocationReminderRepository;
import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
import com.weave.global.exception.BusinessException;
import com.weave.global.exception.ErrorCode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocationReminderService {

  private final LocationReminderRepository locationReminderRepository;
  private final UserRepository userRepository;

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
}
