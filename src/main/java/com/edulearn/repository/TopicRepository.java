package com.edulearn.repository;

import com.edulearn.entity.Subject;
import com.edulearn.entity.Topic;
import com.edulearn.entity.User;
import com.edulearn.enums.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TopicRepository extends JpaRepository<Topic, UUID> {
    List<Topic> findBySubject(Subject subject);
    List<Topic> findBySubjectAndStatus(Subject subject, ContentStatus status);
    Page<Topic> findByStatus(ContentStatus status, Pageable pageable);
    Page<Topic> findByCreatedBy(User createdBy, Pageable pageable);
    Page<Topic> findByStatusAndCreatedBy(ContentStatus status, User createdBy, Pageable pageable);
    List<Topic> findByCreatedByOrderByUpdatedAtDesc(User createdBy);
    List<Topic> findByStatusAndCreatedByOrderByUpdatedAtDesc(ContentStatus status, User createdBy);
    long countByStatus(ContentStatus status);
    long countByCreatedBy(User createdBy);
    long countByStatusAndCreatedBy(ContentStatus status, User createdBy);
}
