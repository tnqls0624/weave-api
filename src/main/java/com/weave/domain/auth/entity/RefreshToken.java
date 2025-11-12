package com.weave.domain.auth.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "refreshToken", timeToLive = 604800) // 7일
public class RefreshToken {

  @Id
  private String id;

  @Indexed
  private String email;

  private String refreshToken;

  private LocalDateTime expiresAt;
}
