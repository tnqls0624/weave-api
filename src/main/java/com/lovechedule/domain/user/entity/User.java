package com.lovechedule.domain.user.entity;// User.java

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.*;

import java.util.Date;

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
    @Indexed(unique = true)
    private String fcmToken;

    @Field("push_enabled")
    @Builder.Default
    private boolean pushEnabled = true;

    @Field("schedule_alarm")
    @Builder.Default
    private boolean scheduleAlarm = true;

    @Field("anniversary_alarm")
    @Builder.Default
    private boolean anniversaryAlarm = true;

    @CreatedDate
    private Date createdAt;

    @LastModifiedDate
    private Date updatedAt;
}
