package com.weave.global.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import java.io.IOException;
import java.time.Duration;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
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

    // 커넥션 풀 설정
    GenericObjectPoolConfig<Object> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(50);          // 최대 커넥션 수
    poolConfig.setMaxIdle(25);           // 최대 유휴 커넥션
    poolConfig.setMinIdle(5);            // 최소 유휴 커넥션
    poolConfig.setMaxWait(Duration.ofMillis(3000));  // 커넥션 대기 시간
    poolConfig.setTestOnBorrow(true);    // 커넥션 검증
    poolConfig.setTestWhileIdle(true);   // 유휴 커넥션 검증

    // 소켓 옵션
    SocketOptions socketOptions = SocketOptions.builder()
        .connectTimeout(Duration.ofSeconds(3))
        .keepAlive(true)
        .build();

    // 클라이언트 옵션
    ClientOptions clientOptions = ClientOptions.builder()
        .socketOptions(socketOptions)
        .autoReconnect(true)
        .build();

    // Lettuce 클라이언트 설정
    LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
        .poolConfig(poolConfig)
        .clientOptions(clientOptions)
        .commandTimeout(Duration.ofSeconds(5))
        .build();

    return new LettuceConnectionFactory(config, clientConfig);
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate(
      RedisConnectionFactory connectionFactory,
      ObjectMapper redisObjectMapper) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper));
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper));
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

    // Date를 ISO-8601 형식으로 직렬화
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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
