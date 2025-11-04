package com.weave.domain.schedule.entity;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Document(collection = "schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {

    @Id
    private ObjectId id;

    @Field("title")
    @NotBlank
    private String title;

    @Field("memo")
    private String memo;

    @Field("is_anniversary")
    @NotNull
    @Builder.Default
    private Boolean isAnniversary = false;

    @Field("start_date")
    @NotBlank
    private String startDate;

    @Field("end_date")
    @NotBlank
    private String endDate;

    @Field("repeat_type")
    @Builder.Default
    private RepeatType repeatType = RepeatType.NONE;

    @Field("participants")
    @Builder.Default
    private List<ObjectId> participants = List.of();

    @Field("calendar_type")
    @Builder.Default
    private CalendarType calendarType = CalendarType.SOLAR;

    @Field("workspace")
    @DBRef
    private ObjectId workspace;

    @CreatedDate
    private Date createdAt;

    @LastModifiedDate
    private Date updatedAt;

    public enum RepeatType {
        NONE("none"),
        MONTHLY("monthly"),
        YEARLY("yearly");

        private final String value;

        RepeatType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum CalendarType {
        SOLAR("solar"),
        LUNAR("lunar");

        private final String value;

        CalendarType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
