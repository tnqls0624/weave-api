package com.weave.global.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@Configuration
public class RedisConfig {

  @Value("${spring.data.redis.host:localhost}")
  private String host;

  @Value("${spring.data.redis.port:6379}")
  private int port;

  @Value("${spring.data.redis.password:}")
  private String password;

  @PostConstruct
  public void logRedisConfig() {
    log.info("=".repeat(80));
    log.info("Redis Configuration");
    log.info("=".repeat(80));
    log.info("Redis Host: {}", host);
    log.info("Redis Port: {}", port);
    log.info("Redis Password: {}", password != null && !password.isEmpty() ? "[MASKED]" : "[EMPTY]");
    log.info("=".repeat(80));
  }

  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
    config.setHostName(host);
    config.setPort(port);
    if (password != null && !password.isEmpty()) {
      config.setPassword(password);
    }
    return new LettuceConnectionFactory(config);
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
    return template;
  }
}
