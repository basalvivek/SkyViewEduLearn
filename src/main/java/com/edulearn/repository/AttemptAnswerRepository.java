package com.edulearn.repository;

import com.edulearn.entity.AttemptAnswer;
import com.edulearn.entity.ExamAttempt;
import com.edulearn.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, UUID> {
    List<AttemptAnswer> findByAttempt(ExamAttempt attempt);
    Optional<AttemptAnswer> findByAttemptAndQuestion(ExamAttempt attempt, Question question);
}
