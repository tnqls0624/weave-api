package com.weave.global.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * 여러 날짜 형식을 허용하는 커스텀 Date Deserializer
 */
public class FlexibleDateDeserializer extends JsonDeserializer<Date> {

  private static final String[] DATE_FORMATS = {
      "yyyy-MM-dd'T'HH:mm:ss",
      "yyyy-MM-dd HH:mm:ss",
      "yyyy-MM-dd'T'HH:mm:ss.SSS",
      "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
      "yyyy-MM-dd"
  };

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
        sdf.setLenient(false);
        return sdf.parse(dateStr);
      } catch (ParseException ignored) {
      }
    }
    throw new IOException("Cannot parse date: " + dateStr + ". Supported formats: " + String.join(", ", DATE_FORMATS));
  }
}
