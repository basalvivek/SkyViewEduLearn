package com.edulearn.repository;

import com.edulearn.entity.Question;
import com.edulearn.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, UUID> {
    List<QuestionOption> findByQuestionOrderByDisplayOrderAsc(Question question);
    void deleteByQuestion(Question question);
}
