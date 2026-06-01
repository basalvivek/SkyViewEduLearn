package com.edulearn.dto.response;

import com.edulearn.entity.Notification;

import java.time.OffsetDateTime;
import java.util.UUID;

public class NotificationResponse {

    private UUID id;
    private String message;
    private boolean isRead;
    private String link;
    private OffsetDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.id = n.getId();
        r.message = n.getMessage();
        r.isRead = n.isRead();
        r.link = n.getLink();
        r.createdAt = n.getCreatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public String getMessage() { return message; }
    public boolean isRead() { return isRead; }
    public String getLink() { return link; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
