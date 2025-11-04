package com.weave.domain.auth.jwt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

  private final PrivateKey privateKey;
  private final PublicKey publicKey;

  @Value("${jwt.expiration:3600000}") // 1시간
  private long validityInMilliseconds;

  public JwtTokenProvider(
      @Value("${jwt.private-key-path:}") String privateKeyPath,
      @Value("${jwt.public-key-path:}") String publicKeyPath
  ) {
    if (privateKeyPath == null || privateKeyPath.isBlank()
        || publicKeyPath == null || publicKeyPath.isBlank()) {
      // 개발환경: RSA 키 쌍을 자동 생성
      try {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();

        System.out.println("⚡ [DEV MODE] 새 RSA 키 자동 생성됨");
        System.out.println("PrivateKey (Base64): " +
            Base64.getEncoder().encodeToString(this.privateKey.getEncoded()));
        System.out.println("PublicKey (Base64): " +
            Base64.getEncoder().encodeToString(this.publicKey.getEncoded()));

      } catch (Exception e) {
        throw new RuntimeException("RSA 키 생성 실패", e);
      }
    } else {
      // 운영환경: pem 파일에서 로딩
      this.privateKey = KeyLoaderUtils.loadPrivateKey(privateKeyPath);
      this.publicKey = KeyLoaderUtils.loadPublicKey(publicKeyPath);
    }
  }

  /**
   * 토큰 생성
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
