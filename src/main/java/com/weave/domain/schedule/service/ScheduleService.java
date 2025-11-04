package com.weave.domain.schedule.service;

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
  private final NotificationService notificationService;

  public ScheduleResponseDto create(CreateRequestScheduleDto dto, String id) {
    // 워크스페이스 찾기
    Workspace workspace = workspaceRepository.findById(new ObjectId(id))
        .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND));

    Schedule schedule = Schedule.builder()
        .workspace(new ObjectId(id))
        .title(dto.getTitle())
        .memo(dto.getMemo())
        .startDate(dto.getStartDate())
        .endDate(dto.getEndDate())
        .repeatType(dto.getRepeatType())
        .participants(
            dto.getParticipants() != null ?
                dto.getParticipants().stream()
                    .map(ObjectId::new)
                    .toList() : null
        )
        .calendarType(dto.getCalendarType())
        .build();

    Schedule savedSchedule = scheduleRepository.save(schedule);

    // 워크스페이스 참여자들에게 알림 발송
    sendScheduleCreatedNotification(workspace, savedSchedule);

    return ScheduleResponseDto.from(savedSchedule);
  }

  /**
   * 일정 생성 시 워크스페이스 참여자들에게 알림 발송
   */
  private void sendScheduleCreatedNotification(Workspace workspace, Schedule schedule) {
    if (workspace.getUsers() == null || workspace.getUsers().isEmpty()) {
      log.info("No users in workspace to notify");
      return;
    }

    // 워크스페이스의 모든 참여자 조회
    List<User> participants = userRepository.findAllById(workspace.getUsers());

    String title = "새로운 일정";
    String body = String.format("'%s' 일정이 등록되었습니다.", schedule.getTitle());

    for (User user : participants) {
      if (user.isScheduleAlarm()) {
        notificationService.sendPushNotification(user, title, body);
      }
    }

    log.info("Sent schedule creation notification to {} users", participants.size());
  }

  public ScheduleResponseDto update(UpdateRequestScheduleDto dto, String id) {
    Schedule schedule = scheduleRepository.findById(new ObjectId(id))
        .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

    schedule.setTitle(dto.getTitle());
    schedule.setMemo(dto.getMemo());
    schedule.setStartDate(dto.getStartDate());
    schedule.setEndDate(dto.getEndDate());
    schedule.setRepeatType(dto.getRepeatType());
    schedule.setCalendarType(dto.getCalendarType());
    if (dto.getParticipants() != null) {
      schedule.setParticipants(
          dto.getParticipants().stream()
              .map(ObjectId::new)
              .toList()
      );
    }

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
