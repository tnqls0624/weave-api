package com.weave.global.config;

import com.weave.domain.auth.jwt.JwtTokenProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserDetailsService userDetailsService;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message,
        StompHeaderAccessor.class);

    if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
      // CONNECT 시 JWT 토큰 검증
      String token = extractToken(accessor);

      if (token != null && jwtTokenProvider.validateToken(token)) {
        try {
          String email = jwtTokenProvider.getEmail(token);
          UserDetails userDetails = userDetailsService.loadUserByUsername(email);

          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(userDetails, null,
                  userDetails.getAuthorities());

          SecurityContextHolder.getContext().setAuthentication(authentication);

          // 세션에 사용자 이메일 저장
          accessor.getSessionAttributes().put("userEmail", email);
          accessor.setUser(authentication);

          log.info("WebSocket authenticated user: {}", email);
        } catch (Exception e) {
          log.error("Error authenticating WebSocket user", e);
          throw new IllegalArgumentException("Authentication failed");
        }
      } else {
        log.warn("WebSocket connection without valid token");
        throw new IllegalArgumentException("Invalid or missing token");
      }
    }

    return message;
  }

  private String extractToken(StompHeaderAccessor accessor) {
    // 1. Authorization 헤더에서 추출
    List<String> authHeaders = accessor.getNativeHeader("Authorization");
    if (authHeaders != null && !authHeaders.isEmpty()) {
      String bearerToken = authHeaders.get(0);
      if (bearerToken.startsWith("Bearer ")) {
        return bearerToken.substring(7);
      }
    }

    // 2. 쿼리 파라미터에서 추출 (클라이언트가 헤더를 보낼 수 없는 경우)
    List<String> tokenParams = accessor.getNativeHeader("token");
    if (tokenParams != null && !tokenParams.isEmpty()) {
      return tokenParams.get(0);
    }

    return null;
  }
}
