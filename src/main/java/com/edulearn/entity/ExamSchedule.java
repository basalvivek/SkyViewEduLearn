package com.edulearn.entity;

import com.edulearn.enums.RecurrenceType;
import com.edulearn.enums.ScheduleStatus;
import com.edulearn.enums.ScheduleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "exam_schedules")
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE exam_schedules SET is_deleted=true, deleted_at=NOW() WHERE id=?")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ── Calendar amendment fields ──────────────────────────────
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "schedule_type", nullable = false)
    private ScheduleType scheduleType = ScheduleType.EXAM;

    @Column(length = 200)
    private String title;

    // exam_id nullable after sql/10-module8-calendar-amendment.sql
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    private Exam exam;

    // class_id nullable after sql/10-module8-calendar-amendment.sql
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private Classroom classroom;

    // ── Drag-drop & recurring fields ───────────────────────────
    @Column(name = "series_id")
    private UUID seriesId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "recurrence_type")
    private RecurrenceType recurrenceType = RecurrenceType.NONE;

    @Column(name = "recurrence_days", length = 20)
    private String recurrenceDays;

    @Column(name = "series_start")
    private LocalDate seriesStart;

    @Column(name = "series_end")
    private LocalDate seriesEnd;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private OffsetDateTime endAt;

    @Column(name = "max_attempts", nullable = false)
    private short maxAttempts = 1;

    @Column(name = "show_results_immediately", nullable = false)
    private boolean showResultsImmediately = false;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private ScheduleStatus status = ScheduleStatus.UPCOMING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
