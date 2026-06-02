package com.edulearn.repository;

import com.edulearn.entity.Classroom;
import com.edulearn.entity.Exam;
import com.edulearn.entity.ExamSchedule;
import com.edulearn.entity.User;
import com.edulearn.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, UUID> {
    List<ExamSchedule> findByClassroomOrderByStartAtAsc(Classroom classroom);
    List<ExamSchedule> findByStatus(ScheduleStatus status);
    List<ExamSchedule> findByExam(Exam exam);

    // Calendar feed — by month window
    List<ExamSchedule> findByStartAtBetweenOrderByStartAtAsc(OffsetDateTime from, OffsetDateTime to);
    List<ExamSchedule> findByClassroomIdAndStartAtBetweenOrderByStartAtAsc(UUID classroomId, OffsetDateTime from, OffsetDateTime to);

    // Teacher-assigned calendar queries
    List<ExamSchedule> findByAssignedToAndStartAtBetweenOrderByStartAtAsc(User assignedTo, OffsetDateTime from, OffsetDateTime to);
    List<ExamSchedule> findByAssignedToAndClassroomIdAndStartAtBetweenOrderByStartAtAsc(User assignedTo, UUID classroomId, OffsetDateTime from, OffsetDateTime to);

    // Series operations
    List<ExamSchedule> findBySeriesIdOrderByStartAtAsc(UUID seriesId);
    List<ExamSchedule> findBySeriesIdAndStartAtGreaterThanEqualOrderByStartAtAsc(UUID seriesId, OffsetDateTime startAt);
}
