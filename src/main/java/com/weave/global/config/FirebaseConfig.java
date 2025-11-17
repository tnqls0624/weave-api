package com.weave.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.weave.global.BusinessException;
import com.weave.global.ErrorCode;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FirebaseConfig {

  @Value("${firebase.config-path:}")
  private String firebaseConfigPath;

  @PostConstruct
  public void initialize() {
    try {
      if (!FirebaseApp.getApps().isEmpty()) {
        return;
      }

      if (firebaseConfigPath == null || firebaseConfigPath.isBlank()) {
        log.warn("Firebase config-path is empty. Skipping Firebase initialization.");
        return;
      }

      File file = new File(firebaseConfigPath);
      if (!file.exists()) {
        log.warn("Firebase config file not found: {}. Skipping Firebase initialization.",
            firebaseConfigPath);
        return;
      }

      try (InputStream serviceAccount = new FileInputStream(file)) {
        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build();

        FirebaseApp.initializeApp(options);
        log.info("Firebase initialized successfully with {}", firebaseConfigPath);
      }
    } catch (Exception e) {
      log.error("Failed to initialize Firebase", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR);
    }
  }
}
