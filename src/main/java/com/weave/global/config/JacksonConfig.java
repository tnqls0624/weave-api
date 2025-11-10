package com.weave.global.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import org.bson.types.ObjectId;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer objectIdCustomizer() {
    return builder -> {
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
    };
  }
}
