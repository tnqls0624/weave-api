package com.weave.global.config;

import com.weave.domain.auth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.messaging.handler.invocation.reactive.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor;
import reactor.core.publisher.Mono;

@Configuration
@EnableRSocketSecurity
@RequiredArgsConstructor
public class RSocketConfig {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserDetailsService userDetailsService;

  @Bean
  public RSocketMessageHandler messageHandler(RSocketStrategies strategies) {
    RSocketMessageHandler handler = new RSocketMessageHandler();
    handler.getArgumentResolverConfigurer()
        .addCustomResolver(new AuthenticationPrincipalArgumentResolver());
    handler.setRSocketStrategies(strategies);
    return handler;
  }

  @Bean
  public PayloadSocketAcceptorInterceptor authorization(RSocketSecurity security) {
    security
        .authorizePayload(authorize -> authorize
            .setup().permitAll() // 초기 연결은 허용
            .route("workspace.*.locations.get").permitAll() // 조회는 인증 불필요
            .anyRequest().authenticated() // 나머지는 인증 필요
            .anyExchange().permitAll()
        )
        .jwt(jwt -> jwt.authenticationManager(jwtAuthenticationManager()));
    return security.build();
  }

  @Bean
  public ReactiveAuthenticationManager jwtAuthenticationManager() {
    return authentication -> {
      String token = authentication.getCredentials().toString();

      if (!jwtTokenProvider.validateToken(token)) {
        return Mono.error(new IllegalArgumentException("Invalid JWT token"));
      }

      String email = jwtTokenProvider.getEmail(token);
      UserDetails userDetails = userDetailsService.loadUserByUsername(email);

      Authentication auth = new UsernamePasswordAuthenticationToken(
          userDetails,
          null,
          userDetails.getAuthorities()
      );

      return Mono.just(auth);
    };
  }
}
