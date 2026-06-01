package com.edulearn.repository;

import com.edulearn.entity.Exam;
import com.edulearn.entity.User;
import com.edulearn.enums.ExamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExamRepository extends JpaRepository<Exam, UUID> {
    List<Exam> findByCreatedByOrderByCreatedAtDesc(User user);
    Page<Exam> findAll(Pageable pageable);
    List<Exam> findByStatus(ExamStatus status);
}
