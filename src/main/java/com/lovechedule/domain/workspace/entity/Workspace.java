package com.lovechedule.domain.workspace.entity;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "workspaces")
public class Workspace {

    @Id
    private ObjectId id;

    @Field("master")
    private ObjectId master;

    @Field("users")
    private List<ObjectId> users;

    private Tag tags;

    @Field("love_day")
    private String loveDay;

    @Field("thumbnail_image")
    private String thumbnailImage;

    @CreatedDate
    private Date createdAt;

    @LastModifiedDate
    private Date updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tag {
        private ColorTag anniversary;
        private ColorTag together;
        private ColorTag guest;
        private ColorTag master;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColorTag {
        private String color;
    }
}
