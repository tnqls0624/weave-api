package com.weave.domain.user.service;

import com.weave.domain.auth.dto.UpdateUserRequestDto;
import com.weave.domain.user.dto.UpdateNotificationRequestDto;
import com.weave.domain.user.dto.UserResponseDto;
import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
import com.weave.global.BusinessException;
import com.weave.global.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

  private final UserRepository userRepository;

  public Boolean existsByInviteCode(String code) {
    return userRepository.existsByInviteCode(code);
  }

  public UserResponseDto updateNotification(UpdateNotificationRequestDto dto, String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    user.setFcmToken(dto.getFcmToken());
    user.setPushEnabled(dto.isPushEnabled());
    user.setAnniversaryAlarm(dto.isAnniversaryAlarm());
    user.setScheduleAlarm(dto.isScheduleAlarm());
    userRepository.save(user);

    return UserResponseDto.from(user);
  }

  // 개인 정보 수정
  public UserResponseDto update(UpdateUserRequestDto dto, String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    user.setName(dto.getName());
    user.setFcmToken(dto.getFcmToken());
    user.setPushEnabled(dto.isPushEnabled());
    user.setAnniversaryAlarm(dto.isAnniversaryAlarm());
    user.setScheduleAlarm(dto.isScheduleAlarm());
    userRepository.save(user);

    return UserResponseDto.from(user);
  }

  public UserResponseDto findByEmail(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    return UserResponseDto.from(user);
  }
}
