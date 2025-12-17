package com.weave.global.service;

import com.weave.domain.user.entity.User;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@RequiredArgsConstructor
@Service
public class DiscordNotificationService {

  private final WebClient webClient;

  @Value("${discord.webhook.url:}")
  private String webhookUrl;

  @Async("notificationExecutor")
  public void sendNewUserNotification(User user) {
    if (webhookUrl == null || webhookUrl.isBlank()) {
      log.debug("Discord webhook URL is not configured, skipping notification");
      return;
    }

    try {
      String message = buildNewUserMessage(user);

      Map<String, Object> payload = new HashMap<>();
      payload.put("content", message);

      webClient.post()
          .uri(webhookUrl)
          .bodyValue(payload)
          .retrieve()
          .bodyToMono(String.class)
          .subscribe(
              response -> log.info("Discord notification sent for new user: {}", user.getEmail()),
              error -> log.error("Failed to send Discord notification: {}", error.getMessage())
          );
    } catch (Exception e) {
      log.error("Error sending Discord notification", e);
    }
  }

  private String buildNewUserMessage(User user) {
    String timestamp = LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    return String.format(
        "🎉 **새로운 유저가 가입했습니다!**\n" +
        "━━━━━━━━━━━━━━━━━━━━\n" +
        "👤 **이름**: %s\n" +
        "📧 **이메일**: %s\n" +
        "🔑 **로그인 타입**: %s\n" +
        "🕐 **가입 시간**: %s",
        user.getName(),
        user.getEmail(),
        user.getLoginType(),
        timestamp
    );
  }
}
