package com.edulearn.repository;

import com.edulearn.entity.ExamAttempt;
import com.edulearn.entity.ExamSchedule;
import com.edulearn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {
    Optional<ExamAttempt> findByScheduleAndStudent(ExamSchedule schedule, User student);
    List<ExamAttempt> findBySchedule(ExamSchedule schedule);
    List<ExamAttempt> findByStudent(User student);
}
