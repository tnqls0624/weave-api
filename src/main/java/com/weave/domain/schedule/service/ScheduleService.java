package com.weave.domain.schedule.service;

import com.google.common.collect.ImmutableList;
import com.weave.domain.schedule.dto.CreateRequestScheduleDto;
import com.weave.domain.schedule.dto.ScheduleResponseDto;
import com.weave.domain.schedule.dto.UpdateRequestScheduleDto;
import com.weave.domain.schedule.entity.Schedule;
import com.weave.domain.schedule.repository.ScheduleRepository;
import com.weave.domain.workspace.entity.Workspace;
import com.weave.domain.workspace.repository.WorkspaceRepository;
import com.weave.global.BusinessException;
import com.weave.global.ErrorCode;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class ScheduleService {

  private final ScheduleRepository scheduleRepository;
  private final WorkspaceRepository workspaceRepository;
  private final ScheduleNotificationService scheduleNotificationService;

  public ScheduleResponseDto create(CreateRequestScheduleDto dto) {
    log.info("create schedule: {}", dto);
    Workspace workspace = workspaceRepository.findById(new ObjectId(dto.getWorkspace()))
        .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND));

    log.info("create schedule for workspace: {}", workspace.getId());

    List<ObjectId> participantIds = Optional.ofNullable(dto.getParticipants())
        .map(participants -> participants.stream()
            .map(ObjectId::new)
            .toList())
        .orElse(ImmutableList.of());

    Schedule schedule = Schedule.builder()
        .workspace(workspace.getId())
        .title(dto.getTitle())
        .memo(dto.getMemo())
        .startDate(dto.getStartDate())
        .endDate(dto.getEndDate())
        .repeatType(String.valueOf(dto.getRepeatType()))
        .participants(participantIds)
        .calendarType(dto.getCalendarType())
        .isAllDay(dto.getIsAllDay() != null ? dto.getIsAllDay() : false)
        .reminderMinutes(dto.getReminderMinutes())
        .reminderSent(false)
        .isImportant(dto.getIsImportant() != null ? dto.getIsImportant() : false)
        .build();

    Schedule savedSchedule = scheduleRepository.save(schedule);

    scheduleNotificationService.sendScheduleCreatedNotification(workspace, savedSchedule);

    return ScheduleResponseDto.from(savedSchedule);
  }

  public ScheduleResponseDto update(UpdateRequestScheduleDto dto, String id) {
    Schedule schedule = scheduleRepository.findById(new ObjectId(id))
        .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

    schedule.setTitle(dto.getTitle());
    schedule.setMemo(dto.getMemo());
    schedule.setStartDate(dto.getStartDate());
    schedule.setEndDate(dto.getEndDate());
    schedule.setRepeatType(String.valueOf(dto.getRepeatType()));
    schedule.setCalendarType(dto.getCalendarType());
    schedule.setIsAllDay(dto.getIsAllDay() != null ? dto.getIsAllDay() : false);
    schedule.setReminderMinutes(dto.getReminderMinutes());
    schedule.setIsImportant(dto.getIsImportant() != null ? dto.getIsImportant() : false);

    // 알림 설정이 변경되면 reminderSent 초기화
    schedule.setReminderSent(false);

    // 중요 일정이 변경되면 D-day 알림 초기화
    if (dto.getIsImportant() != null && dto.getIsImportant()) {
      schedule.setLastDdayNotificationSent(null);
    }

    Optional.ofNullable(dto.getParticipants())
        .ifPresent(participants ->
            schedule.setParticipants(
                participants.stream()
                    .map(ObjectId::new)
                    .toList()
            )
        );

    Schedule updatedSchedule = scheduleRepository.save(schedule);

    scheduleNotificationService.sendScheduleUpdatedNotification(updatedSchedule);

    return ScheduleResponseDto.from(updatedSchedule);
  }

  public ScheduleResponseDto delete(String id) {
    Schedule schedule = scheduleRepository.findById(new ObjectId(id))
        .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

    scheduleNotificationService.sendScheduleDeletedNotification(schedule);

    scheduleRepository.delete(schedule);
    return ScheduleResponseDto.from(schedule);
  }

  public ScheduleResponseDto findById(String id) {
    Schedule schedule = scheduleRepository.findById(new ObjectId(id))
        .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
    return ScheduleResponseDto.from(schedule);
  }
}
