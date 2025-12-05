package com.weave.domain.schedule.service;

import com.google.common.collect.ImmutableList;
import com.weave.domain.checklist.dto.ChecklistItemDto;
import com.weave.domain.checklist.service.ChecklistService;
import com.weave.domain.locationreminder.dto.LocationReminderDto;
import com.weave.domain.locationreminder.service.LocationReminderService;
import com.weave.domain.schedule.dto.CreateRequestScheduleDto;
import com.weave.domain.schedule.dto.ScheduleResponseDto;
import com.weave.domain.schedule.dto.UpdateRequestScheduleDto;
import com.weave.domain.schedule.entity.Schedule;
import com.weave.domain.schedule.repository.ScheduleRepository;
import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
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
  private final UserRepository userRepository;
  private final ScheduleNotificationService scheduleNotificationService;
  private final LocationReminderService locationReminderService;
  private final ChecklistService checklistService;

  public ScheduleResponseDto create(CreateRequestScheduleDto dto, String creatorEmail) {
    log.info("create schedule: {}", dto);
    Workspace workspace = workspaceRepository.findById(new ObjectId(dto.getWorkspace()))
        .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND));

    log.info("create schedule for workspace: {}", workspace.getId());

    // 일정 생성자 조회
    User creator = userRepository.findByEmail(creatorEmail).orElse(null);

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

    // 일정 참여자에게 초대 알림 발송 (생성자 포함하여 워크스페이스에도 알림)
    scheduleNotificationService.sendScheduleCreatedNotification(workspace, savedSchedule);

    // 참여자가 있으면 초대 알림도 별도 발송
    if (!participantIds.isEmpty()) {
      scheduleNotificationService.sendScheduleInviteNotification(savedSchedule, creator);
    }

    return ScheduleResponseDto.from(savedSchedule);
  }

  public ScheduleResponseDto update(UpdateRequestScheduleDto dto, String id) {
    Schedule schedule = scheduleRepository.findById(new ObjectId(id))
        .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

    // 기존 참여자 목록 저장 (초대 알림을 위해)
    List<ObjectId> oldParticipants = schedule.getParticipants() != null
        ? List.copyOf(schedule.getParticipants())
        : List.of();

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

    // 기존 참여자 목록과 함께 알림 발송 (새로운 참여자에게는 초대 알림)
    scheduleNotificationService.sendScheduleUpdatedNotification(updatedSchedule, oldParticipants);

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

    // 위치 알림 정보 조회
    LocationReminderDto locationReminder = locationReminderService.getReminder(id);

    // 체크리스트 조회
    List<ChecklistItemDto> checklist = checklistService.getChecklist(id);

    return ScheduleResponseDto.from(schedule, locationReminder, checklist);
  }
}
