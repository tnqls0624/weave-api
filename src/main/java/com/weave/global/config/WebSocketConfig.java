package com.weave.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

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
    registry.addEndpoint("/api/ws")
        .setAllowedOriginPatterns("*");

    registry.addEndpoint("/api/ws")
        .setAllowedOriginPatterns("*")
        .withSockJS();
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(webSocketAuthInterceptor);
  }
}
