package com.weave.global.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@Configuration
@EnableRedisRepositories(basePackages = "com.weave.domain.auth.repository")
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
    log.info("Redis Password: {}",
        password != null && !password.isEmpty() ? "[MASKED]" : "[EMPTY]");
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

  @Bean
  public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
      ReactiveRedisConnectionFactory connectionFactory) {
    StringRedisSerializer serializer = new StringRedisSerializer();
    RedisSerializationContext<String, String> serializationContext = RedisSerializationContext
        .<String, String>newSerializationContext()
        .key(serializer)
        .value(serializer)
        .hashKey(serializer)
        .hashValue(serializer)
        .build();
    return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
  }

  @Bean
  public ObjectMapper redisObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();

    // Java 8 date/time 모듈 등록
    mapper.findAndRegisterModules();

    // ObjectId를 String으로 직렬화
    SimpleModule module = new SimpleModule();
    module.addSerializer(org.bson.types.ObjectId.class,
        com.fasterxml.jackson.databind.ser.std.ToStringSerializer.instance);
    module.addDeserializer(org.bson.types.ObjectId.class,
        new JsonDeserializer<org.bson.types.ObjectId>() {
          @Override
          public org.bson.types.ObjectId deserialize(JsonParser p, DeserializationContext ctxt)
              throws IOException {
            String value = p.getValueAsString();
            return (value != null && org.bson.types.ObjectId.isValid(value))
                ? new org.bson.types.ObjectId(value)
                : null;
          }
        });
    mapper.registerModule(module);

    return mapper;
  }
}
