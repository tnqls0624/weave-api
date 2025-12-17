package com.weave.domain.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weave.domain.auth.dto.KakaoUserResponseDto;
import com.weave.domain.auth.dto.LoginRequestDto;
import com.weave.domain.auth.dto.LoginResponseDto;
import com.weave.domain.auth.dto.RefreshTokenRequestDto;
import com.weave.domain.auth.dto.SocialLoginRequestDto;
import com.weave.domain.auth.dto.SocialLoginResponseDto;
import com.weave.domain.auth.dto.TestLoginRequestDto;
import com.weave.domain.auth.dto.TestTokenResponseDto;
import com.weave.domain.auth.entity.RefreshToken;
import com.weave.domain.auth.jwt.JwtTokenProvider;
import com.weave.domain.auth.repository.AuthRepository;
import com.weave.domain.auth.repository.RefreshTokenRepository;
import com.weave.domain.user.entity.LoginType;
import com.weave.domain.user.entity.User;
import com.weave.domain.workspace.entity.Workspace;
import com.weave.domain.workspace.repository.WorkspaceRepository;
import com.weave.global.BusinessException;
import com.weave.global.ErrorCode;
import com.weave.global.service.DiscordNotificationService;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.RSAPublicKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
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
  private final ObjectMapper objectMapper;
  private final WorkspaceRepository workspaceRepository;
  private final DiscordNotificationService discordNotificationService;

  @Value("${app.test-account.email:test@weave.com}")
  private String testAccountEmail;

  @Value("${app.test-account.password:WeaveTest2024!}")
  private String testAccountPassword;

  // 이메일 로그인
  public LoginResponseDto login(LoginRequestDto dto) {
    User user = authRepository.findByEmailAndDeletedFalse(dto.getEmail())
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
        case APPLE -> user = handleAppleLogin(socialLoginRequestDto, inviteCode);
        case GOOGLE, FACEBOOK, NAVER ->
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

    log.info("[SocialLogin] accessToken: {}, refreshToken: {}", accessToken, refreshToken);

    // Redis에 Refresh Token 저장
    saveRefreshToken(user.getEmail(), refreshToken);

    log.info("[SocialLogin] cacheRefreshToken Success");
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

    // 디버깅: Kakao API 응답 확인
    log.info("Kakao login - id: {}, email: '{}', nickname: {}", kakao.getId(), email, nickname);

    // Email이 없으면 에러 발생
    if (email == null || email.isBlank()) {
      log.error("Kakao account email is null. kakao_id: {}, nickname: {}", kakao.getId(), nickname);
      throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED,
          "카카오 계정의 이메일을 가져올 수 없습니다. 카카오 앱 설정에서 이메일 동의항목을 확인해주세요.");
    }

    return authRepository.findByEmail(email)
        .map(user -> {
            // 기존 사용자가 소프트 삭제된 경우 재활성화
            if (Boolean.TRUE.equals(user.getDeleted())) {
                user.setDeleted(false);
                user.setDeletedAt(null);
                authRepository.save(user);
                log.info("Reactivated soft-deleted user: {}", user.getEmail());
            }
            return user;
        })
        .orElseGet(() -> {
            // 신규 사용자 생성
            User newUser = authRepository.save(
                User.builder()
                    .email(email)
                    .name(nickname)
                    .loginType(dto.getLoginType())
                    .inviteCode(inviteCode)
                    .pushEnabled(true)
                    .build()
            );

            // 신규 사용자를 위한 기본 워크스페이스 생성
            createDefaultWorkspace(newUser);
            log.info("Created default workspace for new user: {}", newUser.getEmail());

            // Discord 알림 전송
            discordNotificationService.sendNewUserNotification(newUser);

            return newUser;
        });
  }

  /**
   * 신규 사용자를 위한 기본 워크스페이스 생성
   */
  private void createDefaultWorkspace(User user) {
    Workspace workspace = Workspace.builder()
        .title(user.getName() + "의 캘린더")
        .master(user.getId())
        .users(List.of(user.getId()))
        .build();
    workspaceRepository.save(workspace);
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

  /**
   * Apple 로그인 처리
   * Apple identity token (JWT)을 검증하고 사용자 정보를 추출
   */
  private User handleAppleLogin(SocialLoginRequestDto dto, String inviteCode) {
    try {
      String identityToken = dto.getAccessToken();

      // JWT 디코딩 (검증 없이 페이로드 추출)
      String[] parts = identityToken.split("\\.");
      if (parts.length != 3) {
        throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED, "잘못된 Apple 토큰 형식입니다.");
      }

      // Header에서 kid 추출
      String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
      JsonNode header = objectMapper.readTree(headerJson);
      String kid = header.get("kid").asText();

      // Apple 공개키로 토큰 검증
      verifyAppleToken(identityToken, kid);

      // Payload 디코딩
      String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
      JsonNode payload = objectMapper.readTree(payloadJson);

      String appleUserId = payload.get("sub").asText();
      String email = payload.has("email") ? payload.get("email").asText() : null;

      log.info("Apple login - sub: {}, email: '{}'", appleUserId, email);

      // 이메일이 없는 경우 Apple User ID 기반 이메일 생성
      if (email == null || email.isBlank()) {
        email = appleUserId + "@privaterelay.appleid.com";
        log.info("Apple login - email not provided, using generated email: {}", email);
      }

      String finalEmail = email;
      return authRepository.findByEmail(email)
          .map(user -> {
              // 기존 사용자가 소프트 삭제된 경우 재활성화
              if (Boolean.TRUE.equals(user.getDeleted())) {
                  user.setDeleted(false);
                  user.setDeletedAt(null);
                  authRepository.save(user);
                  log.info("Reactivated soft-deleted user: {}", user.getEmail());
              }
              return user;
          })
          .orElseGet(() -> {
              // 신규 사용자 생성
              User newUser = authRepository.save(
                  User.builder()
                      .email(finalEmail)
                      .name("Apple User")
                      .loginType("APPLE")
                      .inviteCode(inviteCode)
                      .pushEnabled(true)
                      .build()
              );

              // 신규 사용자를 위한 기본 워크스페이스 생성
              createDefaultWorkspace(newUser);
              log.info("Created default workspace for new user: {}", newUser.getEmail());

              // Discord 알림 전송
              discordNotificationService.sendNewUserNotification(newUser);

              return newUser;
          });
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("Apple login failed", e);
      throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED, "Apple 로그인 처리 중 오류가 발생했습니다.");
    }
  }

  /**
   * Apple 공개키로 토큰 검증
   */
  private void verifyAppleToken(String token, String kid) {
    try {
      // Apple 공개키 가져오기
      String keysJson = webClient.get()
          .uri("https://appleid.apple.com/auth/keys")
          .retrieve()
          .bodyToMono(String.class)
          .block();

      JsonNode keysNode = objectMapper.readTree(keysJson);
      JsonNode keys = keysNode.get("keys");

      JsonNode matchingKey = null;
      for (JsonNode key : keys) {
        if (kid.equals(key.get("kid").asText())) {
          matchingKey = key;
          break;
        }
      }

      if (matchingKey == null) {
        throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED, "Apple 공개키를 찾을 수 없습니다.");
      }

      // RSA 공개키 생성
      String n = matchingKey.get("n").asText();
      String e = matchingKey.get("e").asText();

      byte[] nBytes = Base64.getUrlDecoder().decode(n);
      byte[] eBytes = Base64.getUrlDecoder().decode(e);

      BigInteger modulus = new BigInteger(1, nBytes);
      BigInteger exponent = new BigInteger(1, eBytes);

      RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
      KeyFactory factory = KeyFactory.getInstance("RSA");
      PublicKey publicKey = factory.generatePublic(spec);

      // JWT 검증
      io.jsonwebtoken.Jwts.parser()
          .verifyWith((java.security.interfaces.RSAPublicKey) publicKey)
          .build()
          .parseSignedClaims(token);

      log.info("Apple token verified successfully");
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("Apple token verification failed", ex);
      throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED, "Apple 토큰 검증에 실패했습니다.");
    }
  }

  /**
   * 테스트 계정 로그인 (App Store 심사용)
   */
  public SocialLoginResponseDto testAccountLogin(TestLoginRequestDto dto) {
    log.info("Test account login attempt - email: {}", dto.getEmail());

    // 테스트 계정 검증
    if (!testAccountEmail.equals(dto.getEmail()) || !testAccountPassword.equals(dto.getPassword())) {
      throw new BusinessException(ErrorCode.INVALID_LOGIN, "테스트 계정 정보가 일치하지 않습니다.");
    }

    final String inviteCode = generateInviteCode();

    // 테스트 사용자 조회 또는 생성
    User user = authRepository.findByEmail(testAccountEmail)
        .orElseGet(() -> {
            // 신규 사용자 생성
            User newUser = authRepository.save(
                User.builder()
                    .email(testAccountEmail)
                    .name("Test User")
                    .loginType("TEST")
                    .inviteCode(inviteCode)
                    .pushEnabled(false)
                    .build()
            );

            // 신규 사용자를 위한 기본 워크스페이스 생성
            createDefaultWorkspace(newUser);
            log.info("Created default workspace for test user: {}", newUser.getEmail());

            return newUser;
        });

    String accessToken = jwtTokenProvider.createToken(user.getEmail());
    String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

    saveRefreshToken(user.getEmail(), refreshToken);

    log.info("Test account login success - userId: {}", user.getId());

    return SocialLoginResponseDto.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .build();
  }
}
