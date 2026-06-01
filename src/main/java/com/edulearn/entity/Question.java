package com.edulearn.entity;

import com.edulearn.enums.CodeLanguage;
import com.edulearn.enums.ContentStatus;
import com.edulearn.enums.ImageAnswerType;
import com.edulearn.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "questions")
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE questions SET is_deleted=true, deleted_at=NOW() WHERE id=?")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Question implements ApprovableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Column(nullable = false)
    private Short marks = 1;

    @Column(name = "answer_guide", columnDefinition = "TEXT")
    private String answerGuide;

    @Column(name = "answer_explanation", columnDefinition = "TEXT")
    private String answerExplanation;

    @Column(name = "marking_scheme", columnDefinition = "TEXT")
    private String markingScheme;

    @Column(name = "word_limit")
    private Integer wordLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_answer_type", length = 10)
    private ImageAnswerType imageAnswerType;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<QuestionOption> options = new ArrayList<>();

    @Column(name = "correct_boolean")
    private Boolean correctBoolean;

    @Column(name = "model_answer", columnDefinition = "TEXT")
    private String modelAnswer;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "code_lang")
    private CodeLanguage codeLang;

    @Column(name = "starter_code", columnDefinition = "TEXT")
    private String starterCode;

    @Column(name = "expected_output", columnDefinition = "TEXT")
    private String expectedOutput;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "image_alt_text", length = 255)
    private String imageAltText;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private ContentStatus status = ContentStatus.DRAFT;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

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
