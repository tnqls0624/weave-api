package com.weave.domain.user.entity;// User.java

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

  @Id
  private ObjectId id;

  @Field("login_type")
  @NotBlank
  private String loginType;

  @NotBlank
  @Email
  @Field("email")
  @Indexed(unique = true)
  private String email;

  @NotBlank
  private String name;

  private String password;

  private String birthday;

  @Field("invite_code")
  @NotBlank
  @Indexed(unique = true)
  private String inviteCode;

  @Field("fcm_token")
  @Indexed(unique = true, sparse = true)
  private String fcmToken;

  @Field("push_enabled")
  @Builder.Default
  private Boolean pushEnabled = true;

  @Field("location_enabled")
  @Builder.Default
  private Boolean locationEnabled = false;

  @Field("phishing_guard_enabled")
  @Builder.Default
  private Boolean phishingGuardEnabled = false;

  @Field("phishing_auto_block")
  @Builder.Default
  private Boolean phishingAutoBlock = false;

  @Field("phishing_sensitivity_level")
  @Builder.Default
  private String phishingSensitivityLevel = "medium";

  @Field("thumbnail_image")
  private String avatarUrl;

  @CreatedDate
  private Date createdAt;

  @LastModifiedDate
  private Date updatedAt;
}
