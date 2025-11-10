package com.weave.domain.user.entity;// User.java

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Date;
import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "users")
@Data
@Builder
public class User {

  @Id
  private ObjectId id;

  @Field("login_type")
  @NotBlank
  private String loginType;

  @NotBlank
  @Email
  @Indexed(unique = true)                 // unique: true
  private String email;

  @NotBlank
  private String name;

  // EMAIL 로그인 타입에서만 사용 (nullable)
  private String password;

  private String gender;       // optional

  private String birthday;     // optional(문자열로 동일 대응)

  @Field("invite_code")
  @NotBlank
  @Indexed(unique = true)
  private String inviteCode;

  @Field("thumbnail_image")
  private String thumbnailImage;

  @Field("fcm_token")
  @Indexed(unique = true, sparse = true)
  private String fcmToken;

  @Field("push_enabled")
  @Builder.Default
  private boolean pushEnabled = true;

  private String avatarUrl;

  @CreatedDate
  private Date createdAt;

  @LastModifiedDate
  private Date updatedAt;
}
