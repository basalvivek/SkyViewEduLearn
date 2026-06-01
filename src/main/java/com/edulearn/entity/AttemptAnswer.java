package com.edulearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "attempt_answers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_question_id")
    private ExamQuestion examQuestion;

    /**
     * JSON string of selected option UUIDs, e.g. ["uuid1","uuid2"]
     * Stored as TEXT to avoid PostgreSQL array type complexity.
     */
    @Column(name = "selected_option_ids", columnDefinition = "TEXT")
    private String selectedOptionIds;

    @Column(name = "boolean_answer")
    private Boolean booleanAnswer;

    @Column(name = "text_answer", columnDefinition = "TEXT")
    private String textAnswer;

    @Column(name = "marks_awarded")
    private Short marksAwarded;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "is_flagged", nullable = false)
    private boolean isFlagged = false;

    @Column(name = "marker_note", columnDefinition = "TEXT")
    private String markerNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by")
    private User markedBy;

    @Column(name = "marked_at")
    private OffsetDateTime markedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
