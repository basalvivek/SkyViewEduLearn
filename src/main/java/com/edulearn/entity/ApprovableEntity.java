package com.edulearn.entity;

import com.edulearn.enums.ContentStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface ApprovableEntity {
    UUID getId();
    ContentStatus getStatus();
    void setStatus(ContentStatus status);
    String getRejectionReason();
    void setRejectionReason(String reason);
    User getUpdatedBy();
    void setUpdatedBy(User updatedBy);
    User getApprovedBy();
    void setApprovedBy(User approvedBy);
    OffsetDateTime getApprovedAt();
    void setApprovedAt(OffsetDateTime approvedAt);
}
