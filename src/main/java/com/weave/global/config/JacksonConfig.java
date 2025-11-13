package com.weave.global.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import org.bson.types.ObjectId;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer objectIdCustomizer() {
    return builder -> {
      // ObjectId를 String으로 직렬화
      builder.serializerByType(
          org.bson.types.ObjectId.class,
          com.fasterxml.jackson.databind.ser.std.ToStringSerializer.instance
      );
      builder.deserializerByType(
          org.bson.types.ObjectId.class,
          new JsonDeserializer<ObjectId>() {
            @Override
            public ObjectId deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
              String value = p.getValueAsString();
              return (value != null && ObjectId.isValid(value))
                  ? new ObjectId(value)
                  : null;
            }
          }
      );

      // Java 8 date/time 모듈 자동 등록
      builder.findModulesViaServiceLoader(true);

      // Date를 timestamp가 아닌 ISO-8601 문자열로 직렬화
      builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    };
  }

}
