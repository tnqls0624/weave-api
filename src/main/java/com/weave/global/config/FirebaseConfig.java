package com.weave.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.weave.global.BusinessException;
import com.weave.global.ErrorCode;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Slf4j
@Configuration
public class FirebaseConfig {

  @Value("${firebase.config-path}")
  private String firebaseConfigPath;

  @PostConstruct
  public void initialize() {
    try {
      if (FirebaseApp.getApps().isEmpty()) {
        // Firebase 설정 파일이 없으면 초기화 생략
        ClassPathResource resource = new ClassPathResource(firebaseConfigPath);
        if (!resource.exists()) {
          log.warn("Firebase config file not found: {}. Skipping Firebase initialization.", firebaseConfigPath);
          return;
        }

        InputStream serviceAccount = resource.getInputStream();

        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build();

        FirebaseApp.initializeApp(options);
        log.info("Firebase initialized successfully");
      }
    } catch (IOException e) {
      log.error("Failed to initialize Firebase", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR);
    }
  }
}
