package com.weave.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final WebSocketAuthInterceptor webSocketAuthInterceptor;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    // 클라이언트로 메시지를 보낼 때 prefix
    registry.enableSimpleBroker("/topic", "/queue");

    // 클라이언트에서 메시지를 보낼 때 prefix
    registry.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    log.info("Registering WebSocket endpoints");
    try {
      // spring.mvc.servlet.path=/api 때문에 실제 경로는 /ws로 등록
      registry
          .addEndpoint("/ws")
          .setAllowedOriginPatterns("*");

      registry.addEndpoint("/ws")
          .setAllowedOriginPatterns("*")
          .withSockJS();
      log.info("WebSocket endpoints registered successfully");
    } catch (Exception e) {
      log.error("Failed to register WebSocket endpoints", e);
      throw e;
    }
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    log.info("Configuring WebSocket inbound channel with auth interceptor");
    if (webSocketAuthInterceptor != null) {
      registration.interceptors(webSocketAuthInterceptor);
    } else {
      log.warn("WebSocketAuthInterceptor is null!");
    }
  }
}
