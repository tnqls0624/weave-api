package com.weave.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@Slf4j
public class WebClientConfig {

  @Bean
  public WebClient webClient(WebClient.Builder builder) {

    HttpClient httpClient = HttpClient.create()
        .compress(true)
        .responseTimeout(Duration.ofSeconds(10))
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
        .doOnConnected(conn -> conn
            .addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
            .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));

    return builder
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .filter(logRequest())
        .filter(logResponse())
        .build();
  }

  private ExchangeFilterFunction logRequest() {
    return ExchangeFilterFunction.ofRequestProcessor(req -> {
      String auth = req.headers().getFirst("Authorization");
      String masked = (StringUtils.hasText(auth) && auth.startsWith("Bearer "))
          ? "Bearer ****" : auth;
      log.info("[WebClient] {} {} headers: Authorization={}",
          req.method(), req.url(), masked);
      return reactor.core.publisher.Mono.just(req);
    });
  }

  private ExchangeFilterFunction logResponse() {
    return ExchangeFilterFunction.ofResponseProcessor(res -> {
      log.info("[WebClient] <- status={}", res.statusCode());
      return reactor.core.publisher.Mono.just(res);
    });
  }
}