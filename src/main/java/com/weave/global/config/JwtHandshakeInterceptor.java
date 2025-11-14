package com.weave.global.config;

import com.weave.domain.auth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserDetailsService userDetailsService;

  @Override
  public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Map<String, Object> attributes) {
    try {
      String token = null;
      if (request instanceof ServletServerHttpRequest servletReq) {
        var servletRequest = servletReq.getServletRequest();
        // Authorization header
        String auth = servletRequest.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
          token = auth.substring(7);
        }
        // token query parameter fallback
        if (token == null) {
          token = servletRequest.getParameter("token");
        }
      }

      if (token != null && jwtTokenProvider.validateToken(token)) {
        String email = jwtTokenProvider.getEmail(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        Principal principal = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());
        attributes.put("principal", principal);
        attributes.put("userEmail", email);
        log.info("WebSocket handshake authenticated: {}", email);
      } else {
        log.warn("WebSocket handshake without token or invalid token");
      }
    } catch (Exception e) {
      log.error("Handshake auth error", e);
    }
    return true; // allow handshake to proceed either way
  }

  @Override
  public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                             WebSocketHandler wsHandler, Exception exception) {
    // no-op
  }
}
