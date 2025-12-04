package com.weave.domain.schedule.service;

import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.weave.domain.user.entity.User;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  /**
   * 기본 푸시 알림 전송
   */
  public void sendPushNotification(User user, String title, String body) {
    sendPushNotificationWithData(user, title, body, null);
  }

  /**
   * 데이터가 포함된 푸시 알림 전송
   */
  public void sendPushNotificationWithData(User user, String title, String body, Map<String, String> data) {
    if (user.getFcmToken() == null || user.getFcmToken().isEmpty()) {
      log.warn("User {} has no FCM token", user.getId());
      return;
    }

    if (!user.getPushEnabled()) {
      log.info("User {} has disabled push notifications", user.getId());
      return;
    }

    try {
      Message.Builder messageBuilder = Message.builder()
          .setToken(user.getFcmToken())
          .setNotification(Notification.builder()
              .setTitle(title)
              .setBody(body)
              .build())
          .setApnsConfig(ApnsConfig.builder()
              .setAps(Aps.builder()
                  .setSound("default")
                  .build())
              .build());

      // 데이터가 있으면 추가
      if (data != null && !data.isEmpty()) {
        messageBuilder.putAllData(data);
      }

      String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
      log.info("Successfully sent message to user {}: {}", user.getId(), response);
    } catch (FirebaseMessagingException e) {
      log.error("Failed to send message to user {}: {}", user.getId(), e.getMessage());
    }
  }
}
