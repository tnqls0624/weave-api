package com.weave.domain.schedule.service;

import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.weave.domain.user.entity.User;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  public void sendPushNotification(User user, String title, String body) {
    if (user.getFcmToken() == null || user.getFcmToken().isEmpty()) {
      log.warn("User {} has no FCM token", user.getId());
      return;
    }

    if (!user.isPushEnabled()) {
      log.info("User {} has disabled push notifications", user.getId());
      return;
    }

    try {
      Message message = Message.builder()
          .setToken(user.getFcmToken())
          .setNotification(Notification.builder()
              .setTitle(title)
              .setBody(body)
              .build())
          .setApnsConfig(ApnsConfig.builder()
              .setAps(Aps.builder()
                  .setSound("default")
                  .build())
              .build())
          .build();

      String response = FirebaseMessaging.getInstance().send(message);
      log.info("Successfully sent message to user {}: {}", user.getId(), response);
    } catch (FirebaseMessagingException e) {
      log.error("Failed to send message to user {}: {}", user.getId(), e.getMessage());
    }
  }

  public void sendBatchPushNotification(List<User> users, String title, String body) {
    List<Message> messages = new ArrayList<>();

    for (User user : users) {
      if (user.getFcmToken() == null || user.getFcmToken().isEmpty()) {
        continue;
      }

      if (!user.isPushEnabled()) {
        continue;
      }

      Message message = Message.builder()
          .setToken(user.getFcmToken())
          .setNotification(Notification.builder()
              .setTitle(title)
              .setBody(body)
              .build())
          .setApnsConfig(ApnsConfig.builder()
              .setAps(Aps.builder()
                  .setSound("default")
                  .build())
              .build())
          .build();

      messages.add(message);
    }

    if (messages.isEmpty()) {
      log.info("No users to send notifications to");
      return;
    }

    try {
      BatchResponse response = FirebaseMessaging.getInstance().sendEach(messages);
      log.info("Successfully sent {} messages, {} failures",
          response.getSuccessCount(),
          response.getFailureCount());
    } catch (FirebaseMessagingException e) {
      log.error("Failed to send batch messages: {}", e.getMessage());
    }
  }
}
