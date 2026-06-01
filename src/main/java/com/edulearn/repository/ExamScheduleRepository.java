package com.edulearn.repository;

import com.edulearn.entity.Classroom;
import com.edulearn.entity.Exam;
import com.edulearn.entity.ExamSchedule;
import com.edulearn.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, UUID> {
    List<ExamSchedule> findByClassroomOrderByStartAtAsc(Classroom classroom);
    List<ExamSchedule> findByStatus(ScheduleStatus status);
    List<ExamSchedule> findByExam(Exam exam);
}
