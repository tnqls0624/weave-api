package com.weave.domain.auth.service;

import com.weave.domain.auth.dto.*;
import com.weave.domain.auth.jwt.JwtTokenProvider;
import com.weave.domain.auth.repository.AuthRepository;
import com.weave.domain.user.dto.UserResponseDto;
import com.weave.domain.user.entity.LoginType;
import com.weave.domain.user.entity.User;
import com.weave.global.BusinessException;
import com.weave.global.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;
import java.security.SecureRandom;

import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final WebClient webClient;

    // 이메일 로그인
    public LoginResponseDto login(LoginRequestDto dto) {
        User user = authRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        String accessToken = jwtTokenProvider.createToken(user.getEmail());

        return LoginResponseDto.builder().accessToken(accessToken).build();
    }

    // 초대코드 생성
    private String generateInviteCode() {
        char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
        int length = 6;
        return RandomStringUtils.random(length, 0, chars.length, false, false, chars, new SecureRandom());
    }

    public SocialLoginResponseDto socialLogin(SocialLoginRequestDto socialLoginRequestDto) {
        final String inviteCode = generateInviteCode();
        User user = null;

        try {
            switch (LoginType.valueOf(socialLoginRequestDto.getLoginType())) {
                case KAKAO -> user = handleKakaoLogin(socialLoginRequestDto, inviteCode);
                case GOOGLE, FACEBOOK, NAVER, APPLE ->
                        throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED, "아직 지원하지 않는 로그인 타입입니다.");
                default -> throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED);
            }
        } catch (WebClientResponseException e) {
            // HTTP 응답 코드 오류 (401, 400 등)
            log.error("[SocialLogin] 외부 API 오류: {}", e.getResponseBodyAsString(), e);
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED, "소셜 서버 통신 오류 (" + e.getStatusCode() + ")");
        } catch (WebClientRequestException e) {
            // 네트워크 오류 (타임아웃, 연결 실패 등)
            log.error("[SocialLogin] 외부 요청 실패", e);
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED, "소셜 서버에 연결할 수 없습니다.");
        } catch (Exception e) {
            log.error("[SocialLogin] 처리 중 예외 발생", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "소셜 로그인 처리 중 오류가 발생했습니다.");
        }

        String accessToken = jwtTokenProvider.createToken(user.getEmail());

        return SocialLoginResponseDto.builder().accessToken(accessToken).build();

    }

    private User handleKakaoLogin(SocialLoginRequestDto dto, String inviteCode) {
        final String kakaoApiUrl = "https://kapi.kakao.com/v2/user/me";

        KakaoUserResponseDto kakao = webClient.get()
                .uri(kakaoApiUrl)
                .header("Authorization", "Bearer " + dto.getAccessToken())
                .retrieve()
                .bodyToMono(KakaoUserResponseDto.class)
                .block();

        if (kakao == null || kakao.getKakaoAccount() == null) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED, "카카오 계정 정보를 확인할 수 없습니다.");
        }

        var account = kakao.getKakaoAccount();
        var profile  = account.getProfile();

        String email    = account.getEmail();
        String nickname = profile.getNickname();

        String gender   = account.getGender();
        String birthday = account.getBirthday();
        String thumbnailImage = profile.getThumbnailImageUrl();

        return authRepository.findByEmail(email)
                .orElseGet(() ->
                        authRepository.save(
                                User.builder()
                                        .email(email)
                                        .name(nickname)
                                        .loginType(dto.getLoginType())
                                        .inviteCode(inviteCode)
                                        .gender(gender)
                                        .birthday(birthday)
                                        .anniversaryAlarm(true)
                                        .scheduleAlarm(true)
                                        .pushEnabled(true)
                                        .thumbnailImage(thumbnailImage)
                                        .build()
                        )
                );
    }

    // 개인 정보 수정
    public UserResponseDto update(UpdateUserRequestDto dto, String email) {
        User user = authRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setName(dto.getName());
        user.setFcmToken(dto.getFcmToken());
        user.setPushEnabled(dto.isPushEnabled());
        user.setAnniversaryAlarm(dto.isAnniversaryAlarm());
        user.setScheduleAlarm(dto.isScheduleAlarm());
        authRepository.save(user);

        return UserResponseDto.from(user);
    }

    public UserResponseDto findByEmail(String email) {
        User user = authRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserResponseDto.from(user);
    }
}
