package com.weave.global.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.bson.types.ObjectId;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

  private static final String[] DATE_FORMATS = {
      "yyyy-MM-dd'T'HH:mm:ss",
      "yyyy-MM-dd HH:mm:ss",
      "yyyy-MM-dd'T'HH:mm:ss.SSS",
      "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
      "yyyy-MM-dd"
  };

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

      // 여러 날짜 형식을 허용하는 커스텀 Deserializer
      builder.deserializerByType(Date.class, new JsonDeserializer<Date>() {
        @Override
        public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
          String dateStr = p.getValueAsString();
          if (dateStr == null || dateStr.isEmpty()) {
            return null;
          }

          TimeZone tz = TimeZone.getTimeZone("Asia/Seoul");
          for (String format : DATE_FORMATS) {
            try {
              SimpleDateFormat sdf = new SimpleDateFormat(format);
              sdf.setTimeZone(tz);
              return sdf.parse(dateStr);
            } catch (ParseException ignored) {
            }
          }
          throw new IOException("Cannot parse date: " + dateStr);
        }
      });

      // Date를 ISO-8601 형식으로 직렬화
      builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
      builder.simpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    };
  }

}
