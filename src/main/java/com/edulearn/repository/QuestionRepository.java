package com.edulearn.repository;

import com.edulearn.entity.Question;
import com.edulearn.entity.Topic;
import com.edulearn.entity.User;
import com.edulearn.enums.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    List<Question> findByTopic(Topic topic);
    List<Question> findByTopicAndStatus(Topic topic, ContentStatus status);
    Page<Question> findByStatus(ContentStatus status, Pageable pageable);
    Page<Question> findByCreatedBy(User createdBy, Pageable pageable);
    Page<Question> findByStatusAndCreatedBy(ContentStatus status, User createdBy, Pageable pageable);
    List<Question> findByCreatedByOrderByUpdatedAtDesc(User createdBy);
    List<Question> findByStatusAndCreatedByOrderByUpdatedAtDesc(ContentStatus status, User createdBy);
    long countByStatus(ContentStatus status);
    long countByCreatedBy(User createdBy);
    long countByStatusAndCreatedBy(ContentStatus status, User createdBy);
}
