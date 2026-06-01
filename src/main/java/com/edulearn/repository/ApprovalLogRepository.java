package com.edulearn.repository;

import com.edulearn.entity.ApprovalLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApprovalLogRepository extends JpaRepository<ApprovalLog, UUID> {
    List<ApprovalLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);
}
