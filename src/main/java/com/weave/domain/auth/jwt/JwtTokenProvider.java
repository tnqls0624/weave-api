package com.weave.domain.auth.jwt;

import com.google.common.base.Strings;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

  private static final String KEY_DIR = "keys";
  private static final String PRIVATE_KEY_FILE = KEY_DIR + "/private_key.der";
  private static final String PUBLIC_KEY_FILE = KEY_DIR + "/public_key.der";

  private final PrivateKey privateKey;
  private final PublicKey publicKey;

  @Value("${jwt.expiration:3600000}") // 1시간
  private long validityInMilliseconds;

  @Value("${jwt.refresh-expiration:604800000}") // 7일
  private long refreshValidityInMilliseconds;

  public JwtTokenProvider(
      @Value("${jwt.private-key-path:}") String privateKeyPath,
      @Value("${jwt.public-key-path:}") String publicKeyPath
  ) {
    KeyPair keyPair = loadOrGenerateKeyPair(privateKeyPath, publicKeyPath);
    this.privateKey = keyPair.getPrivate();
    this.publicKey = keyPair.getPublic();
  }

  private KeyPair loadOrGenerateKeyPair(String privateKeyPath, String publicKeyPath) {
    // 설정된 경로가 있으면 해당 경로에서 로드
    if (!Strings.isNullOrEmpty(privateKeyPath) && !Strings.isNullOrEmpty(publicKeyPath)) {
      PrivateKey privKey = KeyLoaderUtils.loadPrivateKey(privateKeyPath);
      PublicKey pubKey = KeyLoaderUtils.loadPublicKey(publicKeyPath);
      log.info("✅ RSA 키를 설정된 경로에서 로드했습니다.");
      return new KeyPair(pubKey, privKey);
    }

    // 로컬 파일 시스템에 저장된 키가 있는지 확인
    File privateKeyFile = new File(PRIVATE_KEY_FILE);
    File publicKeyFile = new File(PUBLIC_KEY_FILE);

    if (privateKeyFile.exists() && publicKeyFile.exists()) {
      try {
        PrivateKey privKey = loadPrivateKeyFromFile(privateKeyFile);
        PublicKey pubKey = loadPublicKeyFromFile(publicKeyFile);
        log.info("✅ 기존 RSA 키를 로컬에서 로드했습니다. (서버 재부팅 후에도 유효)");
        return new KeyPair(pubKey, privKey);
      } catch (Exception e) {
        log.error("기존 키 로드 실패, 새 키를 생성합니다.", e);
      }
    }

    // 키가 없으면 새로 생성하고 저장
    try {
      KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
      keyGen.initialize(2048);
      KeyPair newKeyPair = keyGen.generateKeyPair();

      // 키를 파일로 저장
      saveKeysToFile(newKeyPair.getPrivate(), newKeyPair.getPublic());

      log.info("⚡ [DEV MODE] 새 RSA 키 생성 및 저장 완료");
      log.info("Private/Public Key가 {}/ 디렉토리에 저장되었습니다.", KEY_DIR);
      log.info("서버 재부팅 후에도 동일한 키를 사용합니다.");

      return newKeyPair;
    } catch (Exception e) {
      throw new RuntimeException("RSA 키 생성 실패", e);
    }
  }

  private void saveKeysToFile(PrivateKey privateKey, PublicKey publicKey) throws IOException {
    File keyDir = new File(KEY_DIR);
    if (!keyDir.exists()) {
      keyDir.mkdirs();
    }

    try (FileOutputStream privateFos = new FileOutputStream(PRIVATE_KEY_FILE);
        FileOutputStream publicFos = new FileOutputStream(PUBLIC_KEY_FILE)) {

      privateFos.write(privateKey.getEncoded());
      publicFos.write(publicKey.getEncoded());
    }
  }

  private PrivateKey loadPrivateKeyFromFile(File file) throws Exception {
    try (FileInputStream fis = new FileInputStream(file)) {
      byte[] keyBytes = fis.readAllBytes();
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      return kf.generatePrivate(spec);
    }
  }

  private PublicKey loadPublicKeyFromFile(File file) throws Exception {
    try (FileInputStream fis = new FileInputStream(file)) {
      byte[] keyBytes = fis.readAllBytes();
      X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      return kf.generatePublic(spec);
    }
  }

  /**
   * 액세스 토큰 생성
   */
  public String createToken(String username) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + validityInMilliseconds);

    return Jwts.builder()
        .subject(username)
        .issuedAt(now)
        .expiration(validity)
        .signWith(privateKey, Jwts.SIG.RS256)
        .compact();
  }

  /**
   * 리프레시 토큰 생성
   */
  public String createRefreshToken(String username) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + refreshValidityInMilliseconds);

    return Jwts.builder()
        .subject(username)
        .issuedAt(now)
        .expiration(validity)
        .signWith(privateKey, Jwts.SIG.RS256)
        .compact();
  }

  /**
   * 리프레시 토큰 만료 시간 조회 (밀리초)
   */
  public long getRefreshTokenValidityInMilliseconds() {
    return refreshValidityInMilliseconds;
  }

  /**
   * 토큰 검증
   */
  public boolean validateToken(String token) {
    try {
      Jwts.parser()
          .verifyWith(publicKey)
          .build()
          .parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      log.warn("Invalid JWT token: {}", e.getMessage());
      return false;
    }
  }

  /**
   * username 추출
   */
  public String getEmail(String token) {
    return Jwts.parser()
        .verifyWith(publicKey)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }
}
