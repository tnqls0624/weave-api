package com.weave.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.weave.global.BusinessException;
import com.weave.global.ErrorCode;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FirebaseConfig {

  @Value("${firebase.config-json:-}")
  private String configJsonString;

  @PostConstruct
  public void initialize() {
    if (!FirebaseApp.getApps().isEmpty()) {
      return;
    }

    try {
      GoogleCredentials credentials = loadCredentials();

      if (credentials == null) {
        log.warn("⚠ Firebase credentials not provided. Skipping Firebase initialization.");
        return;
      }

      FirebaseOptions options = FirebaseOptions.builder()
          .setCredentials(credentials)
          .build();

      FirebaseApp.initializeApp(options);
      log.info("🔥 Firebase initialized successfully");

    } catch (Exception e) {
      log.error("❌ Failed to initialize Firebase", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR);
    }
  }

  private GoogleCredentials loadCredentials() throws IOException {
    if (configJsonString != null && !configJsonString.isBlank()) {
      log.info("Using inline JSON credentials for Firebase");
      return GoogleCredentials.fromStream(
          new ByteArrayInputStream(configJsonString.getBytes(StandardCharsets.UTF_8))
      );
    }
    return null;
  }
}
