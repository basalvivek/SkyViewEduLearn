package com.edulearn.repository;

import com.edulearn.entity.Exam;
import com.edulearn.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, UUID> {
    List<ExamQuestion> findByExamOrderByDisplayOrderAsc(Exam exam);
    long countByExam(Exam exam);
    void deleteByExam(Exam exam);
}
