package com.edulearn.repository;

import com.edulearn.entity.Category;
import com.edulearn.entity.Subject;
import com.edulearn.entity.User;
import com.edulearn.enums.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    List<Subject> findByCategory(Category category);
    List<Subject> findByCategoryAndStatus(Category category, ContentStatus status);
    Page<Subject> findByStatus(ContentStatus status, Pageable pageable);
    Page<Subject> findByCreatedBy(User createdBy, Pageable pageable);
    Page<Subject> findByStatusAndCreatedBy(ContentStatus status, User createdBy, Pageable pageable);
    List<Subject> findByCreatedByOrderByUpdatedAtDesc(User createdBy);
    List<Subject> findByStatusAndCreatedByOrderByUpdatedAtDesc(ContentStatus status, User createdBy);
    long countByStatus(ContentStatus status);
    long countByCreatedBy(User createdBy);
    long countByStatusAndCreatedBy(ContentStatus status, User createdBy);
}
