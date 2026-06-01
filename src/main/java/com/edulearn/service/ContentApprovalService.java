package com.edulearn.service;

import com.edulearn.entity.ApprovableEntity;
import com.edulearn.entity.ApprovalLog;
import com.edulearn.entity.User;
import com.edulearn.enums.ContentStatus;
import com.edulearn.enums.UserRole;
import com.edulearn.repository.ApprovalLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional
public class ContentApprovalService {

    private final ApprovalLogRepository logRepo;

    public ContentApprovalService(ApprovalLogRepository logRepo) {
        this.logRepo = logRepo;
    }

    public void submit(ApprovableEntity entity, User actor) {
        ContentStatus prev = entity.getStatus();
        if (actor.getRole() == UserRole.ADMIN) {
            entity.setStatus(ContentStatus.APPROVED);
            entity.setApprovedBy(actor);
            entity.setApprovedAt(OffsetDateTime.now());
        } else {
            entity.setStatus(ContentStatus.PENDING);
        }
        entity.setUpdatedBy(actor);
        log(entity, prev, entity.getStatus(), actor, null);
    }

    public void approve(ApprovableEntity entity, User admin, String note) {
        ContentStatus prev = entity.getStatus();
        entity.setStatus(ContentStatus.APPROVED);
        entity.setApprovedBy(admin);
        entity.setApprovedAt(OffsetDateTime.now());
        entity.setRejectionReason(null);
        entity.setUpdatedBy(admin);
        log(entity, prev, ContentStatus.APPROVED, admin, note);
    }

    public void reject(ApprovableEntity entity, User admin, String note) {
        ContentStatus prev = entity.getStatus();
        entity.setStatus(ContentStatus.REJECTED);
        entity.setRejectionReason(note);
        entity.setUpdatedBy(admin);
        log(entity, prev, ContentStatus.REJECTED, admin, note);
    }

    private void log(ApprovableEntity entity, ContentStatus from, ContentStatus to, User actor, String note) {
        String entityType = entity.getClass().getSimpleName().toUpperCase();
        logRepo.save(ApprovalLog.builder()
            .entityType(entityType)
            .entityId(entity.getId())
            .fromStatus(from)
            .toStatus(to)
            .actionedBy(actor)
            .note(note)
            .build());
    }
}
