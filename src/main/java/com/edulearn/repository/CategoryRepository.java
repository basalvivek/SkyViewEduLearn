package com.edulearn.repository;

import com.edulearn.entity.Category;
import com.edulearn.entity.User;
import com.edulearn.enums.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Page<Category> findByStatus(ContentStatus status, Pageable pageable);
    Page<Category> findByCreatedBy(User createdBy, Pageable pageable);
    Page<Category> findByStatusAndCreatedBy(ContentStatus status, User createdBy, Pageable pageable);
    List<Category> findByStatus(ContentStatus status);
    List<Category> findByCreatedByOrderByUpdatedAtDesc(User createdBy);
    List<Category> findByStatusAndCreatedByOrderByUpdatedAtDesc(ContentStatus status, User createdBy);
    long countByStatus(ContentStatus status);
    long countByCreatedBy(User createdBy);
    long countByStatusAndCreatedBy(ContentStatus status, User createdBy);

    @Query("SELECT c FROM Category c WHERE c.status IN ('DRAFT','PENDING','APPROVED','REJECTED') ORDER BY c.createdAt DESC")
    Page<Category> findAllForAdmin(Pageable pageable);
}
