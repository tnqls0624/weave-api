package com.weave.domain.user.service;

import com.weave.domain.location.repository.LocationRepository;
import com.weave.domain.schedule.entity.Schedule;
import com.weave.domain.schedule.repository.ScheduleRepository;
import com.weave.domain.user.dto.UpdateNotificationRequestDto;
import com.weave.domain.user.dto.UpdateUserRequestDto;
import com.weave.domain.user.dto.UserResponseDto;
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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

  private final UserRepository userRepository;
  private final WorkspaceRepository workspaceRepository;
  private final ScheduleRepository scheduleRepository;
  private final LocationRepository locationRepository;

  public Boolean existsByInviteCode(String code) {
    return userRepository.existsByInviteCode(code);
  }

  public UserResponseDto updateNotification(UpdateNotificationRequestDto dto, String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

    user.setPushEnabled(dto.isPushEnabled());

    if (dto.getFcmToken() != null) {
      user.setFcmToken(dto.getFcmToken());
    }

    user.setLocationEnabled(dto.isLocationEnabled());

    userRepository.save(user);
    return UserResponseDto.from(user);
  }

  // 개인 정보 수정 (dto에 값이 담긴 항목만 업데이트)
  public UserResponseDto update(UpdateUserRequestDto dto, String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    if (dto.getName() != null) {
      user.setName(dto.getName());
    }
    if (dto.getFcmToken() != null) {
      user.setFcmToken(dto.getFcmToken());
    }
    if (dto.getAvatarUrl() != null) {
      user.setAvatarUrl(dto.getAvatarUrl());
    }
    if (dto.getPushEnabled() != null) {
      user.setPushEnabled(dto.getPushEnabled());
    }
    if (dto.getLocationEnabled() != null) {
      user.setLocationEnabled(dto.getLocationEnabled());
    }

    userRepository.save(user);

    return UserResponseDto.from(user);
  }

  public UserResponseDto findByEmail(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    return UserResponseDto.from(user);
  }

  /**
   * 회원 탈퇴 - 사용자 및 관련 데이터 삭제 1. 워크스페이스에서 사용자 제거 (master인 경우 워크스페이스 삭제) 2. 일정에서 참여자 제거 (혼자인 일정은 삭제)
   * 3. 위치 정보 삭제 4. 사용자 삭제
   */
  @Transactional
  public void deleteByEmail(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    ObjectId userId = user.getId();
    log.info("회원 탈퇴 시작 - userId: {}, email: {}", userId, email);

    // 1. 워크스페이스 처리
    List<Workspace> workspaces = workspaceRepository.findByUsersContaining(userId);
    for (Workspace workspace : workspaces) {
      if (workspace.getMaster().equals(userId)) {
        // master인 경우: 워크스페이스 삭제
        log.info("워크스페이스 삭제 (master) - workspaceId: {}", workspace.getId());
        // 해당 워크스페이스의 모든 일정도 삭제
        List<Schedule> workspaceSchedules = scheduleRepository.findByWorkspace(workspace.getId());
        scheduleRepository.deleteAll(workspaceSchedules);
        workspaceRepository.delete(workspace);
      } else {
        // 멤버인 경우: 워크스페이스에서 사용자 제거
        log.info("워크스페이스에서 사용자 제거 - workspaceId: {}", workspace.getId());
        workspace.getUsers().remove(userId);
        // participantColors에서도 제거
        if (workspace.getParticipantColors() != null) {
          workspace.getParticipantColors().remove(userId.toString());
        }
        workspaceRepository.save(workspace);
      }
    }

    // 2. 일정에서 참여자 제거
    List<Schedule> schedules = scheduleRepository.findByParticipantsContaining(userId);
    for (Schedule schedule : schedules) {
      schedule.getParticipants().remove(userId);
      if (schedule.getParticipants().isEmpty()) {
        // 참여자가 없으면 일정 삭제
        log.info("일정 삭제 (참여자 없음) - scheduleId: {}", schedule.getId());
        scheduleRepository.delete(schedule);
      } else {
        scheduleRepository.save(schedule);
      }
    }

    // 3. 위치 정보 삭제
    locationRepository.deleteByUserId(userId);

    // 4. 사용자 삭제
    userRepository.delete(user);
    log.info("회원 탈퇴 완료 - userId: {}, email: {}", userId, email);
  }
}
