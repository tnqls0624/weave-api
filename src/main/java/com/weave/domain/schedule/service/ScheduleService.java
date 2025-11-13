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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@RequiredArgsConstructor
@Service
public class ScheduleService {

  private final ScheduleRepository scheduleRepository;
  private final WorkspaceRepository workspaceRepository;
  private final ScheduleNotificationService scheduleNotificationService;

  @Transactional
  public ScheduleResponseDto create(CreateRequestScheduleDto dto) {

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
        .build();

    Schedule savedSchedule = scheduleRepository.save(schedule);

    // 트랜잭션 활성 여부 확인 후 등록
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          scheduleNotificationService.sendScheduleCreatedNotification(workspace, savedSchedule);
        }
      });
    } else {
      // 이 경우는 거의 없겠지만, 안전하게 즉시 비동기 호출
      scheduleNotificationService.sendScheduleCreatedNotification(workspace, savedSchedule);
    }

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

    Optional.ofNullable(dto.getParticipants())
        .ifPresent(participants ->
            schedule.setParticipants(
                participants.stream()
                    .map(ObjectId::new)
                    .toList()
            )
        );

    Schedule updatedSchedule = scheduleRepository.save(schedule);
    return ScheduleResponseDto.from(updatedSchedule);
  }

  public ScheduleResponseDto delete(String id) {
    Schedule schedule = scheduleRepository.findById(new ObjectId(id))
        .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

    scheduleRepository.delete(schedule);
    return ScheduleResponseDto.from(schedule);
  }

  public ScheduleResponseDto findById(String id) {
    Schedule schedule = scheduleRepository.findById(new ObjectId(id))
        .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
    return ScheduleResponseDto.from(schedule);
  }
}
