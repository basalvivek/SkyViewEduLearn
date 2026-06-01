package com.edulearn.dto.response;

import com.edulearn.enums.ContentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class SubmissionResponse {
    private UUID id;          // random row key (for UI dedup)
    private UUID entityId;    // actual content entity UUID
    private String entityType;
    private String name;      // display label
    private String path;      // breadcrumb path
    private ContentStatus status;
    private String rejectionReason;
    private String createdByName;   // who created (used in approval queue)
    private String reviewedByName;  // who reviewed
    private OffsetDateTime submittedAt;
    private OffsetDateTime reviewedAt;

    public SubmissionResponse() {}

    /** Constructor used by ApprovalController for backward-compat */
    public SubmissionResponse(UUID entityId, String entityType, String name,
                               ContentStatus status, String rejectionReason,
                               String path, String createdByName,
                               OffsetDateTime submittedAt, OffsetDateTime updatedAt) {
        this.id = UUID.randomUUID();
        this.entityId = entityId;
        this.entityType = entityType;
        this.name = name;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.path = path;
        this.createdByName = createdByName;
        this.submittedAt = submittedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID v) { this.entityId = v; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String v) { this.entityType = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getPath() { return path; }
    public void setPath(String v) { this.path = v; }
    public ContentStatus getStatus() { return status; }
    public void setStatus(ContentStatus v) { this.status = v; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String v) { this.rejectionReason = v; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String v) { this.createdByName = v; }
    public String getReviewedByName() { return reviewedByName; }
    public void setReviewedByName(String v) { this.reviewedByName = v; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(OffsetDateTime v) { this.submittedAt = v; }
    public OffsetDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(OffsetDateTime v) { this.reviewedAt = v; }
}
