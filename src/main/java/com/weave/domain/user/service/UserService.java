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
import java.util.Date;
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
    User user = userRepository.findByEmailAndDeletedFalse(email)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

    log.info("📱 [Notification Update] email: {}, request: pushEnabled={}, fcmToken={}, locationEnabled={}",
        email, dto.getPushEnabled(), dto.getFcmToken() != null ? "exists" : "null", dto.getLocationEnabled());
    log.info("📱 [Notification Update] BEFORE: pushEnabled={}, locationEnabled={}",
        user.getPushEnabled(), user.getLocationEnabled());

    if (dto.getPushEnabled() != null) {
      user.setPushEnabled(dto.getPushEnabled());
    }

    if (dto.getFcmToken() != null) {
      user.setFcmToken(dto.getFcmToken());
    }

    if (dto.getLocationEnabled() != null) {
      user.setLocationEnabled(dto.getLocationEnabled());
    }

    log.info("📱 [Notification Update] AFTER: pushEnabled={}, locationEnabled={}",
        user.getPushEnabled(), user.getLocationEnabled());

    userRepository.save(user);
    return UserResponseDto.from(user);
  }

  // 개인 정보 수정 (dto에 값이 담긴 항목만 업데이트)
  public UserResponseDto update(UpdateUserRequestDto dto, String email) {
    User user = userRepository.findByEmailAndDeletedFalse(email)
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
    User user = userRepository.findByEmailAndDeletedFalse(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    return UserResponseDto.from(user);
  }

  /**
   * 회원 탈퇴 (소프트 삭제) - 데이터는 보존하고 삭제 표시만 함
   * 1. 워크스페이스에서 사용자 제거 (master인 경우에도 워크스페이스 유지, 멤버가 있으면 첫번째 멤버가 master)
   * 2. 일정에서 참여자 제거
   * 3. 위치 정보 삭제
   * 4. 사용자 소프트 삭제 (deleted=true, deletedAt 설정)
   */
  @Transactional
  public void deleteByEmail(String email) {
    User user = userRepository.findByEmailAndDeletedFalse(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    ObjectId userId = user.getId();
    log.info("회원 탈퇴 시작 (소프트 삭제) - userId: {}, email: {}", userId, email);

    // 1. 워크스페이스 처리
    List<Workspace> workspaces = workspaceRepository.findByUsersContaining(userId);
    for (Workspace workspace : workspaces) {
      workspace.getUsers().remove(userId);

      if (workspace.getMaster().equals(userId)) {
        // master인 경우: 다른 멤버가 있으면 첫번째 멤버를 master로 변경
        if (!workspace.getUsers().isEmpty()) {
          ObjectId newMaster = workspace.getUsers().get(0);
          workspace.setMaster(newMaster);
          log.info("워크스페이스 master 변경 - workspaceId: {}, newMaster: {}", workspace.getId(), newMaster);
        }
        // 멤버가 없으면 워크스페이스는 유지하되 사용자만 제거됨
      }

      // participantColors에서 제거
      if (workspace.getParticipantColors() != null) {
        workspace.getParticipantColors().remove(userId.toString());
      }

      log.info("워크스페이스에서 사용자 제거 - workspaceId: {}", workspace.getId());
      workspaceRepository.save(workspace);
    }

    // 2. 일정에서 참여자 제거
    List<Schedule> schedules = scheduleRepository.findByParticipantsContaining(userId);
    for (Schedule schedule : schedules) {
      schedule.getParticipants().remove(userId);
      // 참여자가 없어도 일정은 삭제하지 않음 (데이터 보존)
      scheduleRepository.save(schedule);
      log.info("일정에서 참여자 제거 - scheduleId: {}", schedule.getId());
    }

    // 3. 위치 정보 삭제 (실시간 위치 데이터는 삭제해도 됨)
    locationRepository.deleteByUserId(userId);

    // 4. 사용자 소프트 삭제
    user.setDeleted(true);
    user.setDeletedAt(new Date());
    userRepository.save(user);

    log.info("회원 탈퇴 완료 (소프트 삭제) - userId: {}, email: {}, deletedAt: {}", userId, email, user.getDeletedAt());
  }
}
