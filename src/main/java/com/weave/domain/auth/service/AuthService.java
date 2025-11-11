package com.weave.domain.auth.service;

import com.weave.domain.auth.dto.KakaoUserResponseDto;
import com.weave.domain.auth.dto.LoginRequestDto;
import com.weave.domain.auth.dto.LoginResponseDto;
import com.weave.domain.auth.dto.RefreshTokenRequestDto;
import com.weave.domain.auth.dto.SocialLoginRequestDto;
import com.weave.domain.auth.dto.SocialLoginResponseDto;
import com.weave.domain.auth.dto.TestTokenResponseDto;
import com.weave.domain.auth.entity.RefreshToken;
import com.weave.domain.auth.jwt.JwtTokenProvider;
import com.weave.domain.auth.repository.AuthRepository;
import com.weave.domain.auth.repository.RefreshTokenRepository;
import com.weave.domain.user.entity.LoginType;
import com.weave.domain.user.entity.User;
import com.weave.global.BusinessException;
import com.weave.global.ErrorCode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
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
  private final RefreshTokenRepository refreshTokenRepository;

  // 이메일 로그인
  public LoginResponseDto login(LoginRequestDto dto) {
    User user = authRepository.findByEmail(dto.getEmail())
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN));

    if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
      throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
    }

    String accessToken = jwtTokenProvider.createToken(user.getEmail());
    String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

    // Redis에 Refresh Token 저장
    saveRefreshToken(user.getEmail(), refreshToken);

    return LoginResponseDto.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .build();
  }

  // 초대코드 생성
  private String generateInviteCode() {
    char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    int length = 6;
    return RandomStringUtils.random(length, 0, chars.length, false, false, chars,
        new SecureRandom());
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
      throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED,
          "소셜 서버 통신 오류 (" + e.getStatusCode() + ")");
    } catch (WebClientRequestException e) {
      // 네트워크 오류 (타임아웃, 연결 실패 등)
      log.error("[SocialLogin] 외부 요청 실패", e);
      throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED, "소셜 서버에 연결할 수 없습니다.");
    } catch (Exception e) {
      log.error("[SocialLogin] 처리 중 예외 발생", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "소셜 로그인 처리 중 오류가 발생했습니다.");
    }

    log.info("Social login - id: {}, email: '{}', inviteCode: {}", user.getId(), user.getEmail(),
        inviteCode);

    String accessToken = jwtTokenProvider.createToken(user.getEmail());
    String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

    // Redis에 Refresh Token 저장
    saveRefreshToken(user.getEmail(), refreshToken);

    return SocialLoginResponseDto.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .build();
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
    var profile = account.getProfile();

    String email = account.getEmail();
    String nickname = profile.getNickname();
    String gender = account.getGender();
    String birthday = account.getBirthday();
    String thumbnailImage = profile.getThumbnailImageUrl();

    // 디버깅: Kakao API 응답 확인
    log.info("Kakao login - id: {}, email: '{}', nickname: {}", kakao.getId(), email, nickname);

    // Email이 없으면 에러 발생
    if (email == null || email.isBlank()) {
      log.error("Kakao account email is null. kakao_id: {}, nickname: {}", kakao.getId(), nickname);
      throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED,
          "카카오 계정의 이메일을 가져올 수 없습니다. 카카오 앱 설정에서 이메일 동의항목을 확인해주세요.");
    }

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
                    .pushEnabled(true)
                    .thumbnailImage(thumbnailImage)
                    .build()
            )
        );
  }

  /**
   * Refresh Token으로 새로운 Access Token 발급
   */
  public LoginResponseDto refreshAccessToken(RefreshTokenRequestDto dto) {
    String refreshToken = dto.getRefreshToken();

    // Refresh Token 유효성 검증
    if (!jwtTokenProvider.validateToken(refreshToken)) {
      throw new BusinessException(ErrorCode.INVALID_TOKEN);
    }

    // Redis에서 Refresh Token 조회
    RefreshToken storedToken = refreshTokenRepository.findByRefreshToken(refreshToken)
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

    // 만료 시간 확인
    if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
      refreshTokenRepository.delete(storedToken);
      throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
    }

    String email = jwtTokenProvider.getEmail(refreshToken);

    // 새로운 Access Token 생성
    String newAccessToken = jwtTokenProvider.createToken(email);

    return LoginResponseDto.builder()
        .accessToken(newAccessToken)
        .refreshToken(refreshToken)
        .build();
  }

  /**
   * 로그아웃 - Refresh Token 삭제
   */
  public void logout(String email) {
    refreshTokenRepository.deleteByEmail(email);
  }

  /**
   * Refresh Token을 Redis에 저장
   */
  private void saveRefreshToken(String email, String refreshToken) {
    // 기존 토큰이 있으면 삭제
    refreshTokenRepository.findByEmail(email).ifPresent(refreshTokenRepository::delete);

    long expirationMillis = jwtTokenProvider.getRefreshTokenValidityInMilliseconds();
    LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expirationMillis / 1000);

    RefreshToken token = RefreshToken.builder()
        .email(email)
        .refreshToken(refreshToken)
        .expiresAt(expiresAt)
        .build();

    refreshTokenRepository.save(token);
  }

  /**
   * 테스트용 1년짜리 액세스 토큰 생성
   */
  public TestTokenResponseDto generateTestToken(String email) {
    log.info("generate test token for email: {}", email);
    // 사용자 존재 여부 확인
    authRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    String accessToken = jwtTokenProvider.createToken(email);

    LocalDateTime expiresAt = LocalDateTime.now()
        .plusSeconds(jwtTokenProvider.getRefreshTokenValidityInMilliseconds() / 1000);

    return TestTokenResponseDto.builder()
        .accessToken(accessToken)
        .expiresAt(expiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .message("1년 유효한 테스트용 액세스 토큰입니다. 서버 재부팅 후에도 사용 가능합니다.")
        .build();
  }
}
