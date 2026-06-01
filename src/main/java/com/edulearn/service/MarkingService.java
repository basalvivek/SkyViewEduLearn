package com.edulearn.service;

import com.edulearn.dto.request.MarkAnswerRequest;
import com.edulearn.dto.response.AttemptMarkingResponse;
import com.edulearn.dto.response.MarkingQueueResponse;
import com.edulearn.entity.*;
import com.edulearn.enums.AttemptStatus;
import com.edulearn.enums.QuestionType;
import com.edulearn.exception.ForbiddenException;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MarkingService {

    private final ExamAttemptRepository attemptRepo;
    private final AttemptAnswerRepository answerRepo;
    private final UserRepository userRepo;
    private final QuestionRepository questionRepo;
    private final ExamQuestionRepository examQuestionRepo;
    private final ExamRepository examRepo;
    private final AutoMarkingService autoMarkingService;

    public MarkingService(ExamAttemptRepository attemptRepo,
                          AttemptAnswerRepository answerRepo,
                          UserRepository userRepo,
                          QuestionRepository questionRepo,
                          ExamQuestionRepository examQuestionRepo,
                          ExamRepository examRepo,
                          AutoMarkingService autoMarkingService) {
        this.attemptRepo = attemptRepo;
        this.answerRepo = answerRepo;
        this.userRepo = userRepo;
        this.questionRepo = questionRepo;
        this.examQuestionRepo = examQuestionRepo;
        this.examRepo = examRepo;
        this.autoMarkingService = autoMarkingService;
    }

    @Transactional(readOnly = true)
    public List<MarkingQueueResponse> getQueue(String email) {
        // Return all SUBMITTED attempts that have at least one unawarded manual question
        List<ExamAttempt> submitted = attemptRepo.findAll().stream()
                .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED)
                .collect(Collectors.toList());

        return submitted.stream()
                .map(attempt -> {
                    List<AttemptAnswer> answers = answerRepo.findByAttempt(attempt);
                    int autoMarked = (int) answers.stream()
                            .filter(a -> a.getMarksAwarded() != null).count();
                    int manualPending = (int) answers.stream()
                            .filter(a -> a.getMarksAwarded() == null
                                    && needsManualMarking(a.getQuestion().getQuestionType())).count();
                    boolean isComplete = manualPending == 0;
                    String className = attempt.getSchedule().getClassroom().getName();

                    return new MarkingQueueResponse(
                            attempt.getId(),
                            attempt.getStudent().getFullName(),
                            className,
                            attempt.getSchedule().getExam().getName(),
                            attempt.getSubmittedAt(),
                            answers.size(),
                            autoMarked,
                            manualPending,
                            isComplete
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AttemptMarkingResponse getAttemptForMarking(UUID attemptId, String email) {
        ExamAttempt attempt = findAttemptById(attemptId);
        List<AttemptAnswer> answers = answerRepo.findByAttempt(attempt);
        int autoMarked = (int) answers.stream()
                .filter(a -> a.getMarksAwarded() != null && !needsManualMarking(a.getQuestion().getQuestionType()))
                .count();

        List<AttemptMarkingResponse.MarkingAnswerItem> items = answers.stream().map(answer -> {
            Question q = answer.getQuestion();
            int maxMarks = q.getMarks() != null ? q.getMarks() : 1;

            List<String> selectedOptionTexts = null;
            if (answer.getSelectedOptionIds() != null) {
                List<UUID> ids = autoMarkingService.parseSelectedOptionIds(answer.getSelectedOptionIds());
                selectedOptionTexts = ids.stream()
                        .map(id -> q.getOptions().stream()
                                .filter(opt -> opt.getId().equals(id))
                                .findFirst()
                                .map(QuestionOption::getOptionText)
                                .orElse(id.toString()))
                        .collect(Collectors.toList());
            }

            return new AttemptMarkingResponse.MarkingAnswerItem(
                    answer.getId(),
                    q.getId(),
                    q.getQuestionText(),
                    q.getQuestionType(),
                    maxMarks,
                    answer.getTextAnswer(),
                    selectedOptionTexts,
                    answer.getBooleanAnswer(),
                    answer.getMarksAwarded(),
                    answer.getIsCorrect(),
                    answer.getMarkerNote(),
                    q.getModelAnswer(),
                    q.getMarkingScheme()
            );
        }).collect(Collectors.toList());

        return new AttemptMarkingResponse(
                attempt.getId(),
                attempt.getStudent().getFullName(),
                attempt.getSchedule().getExam().getName(),
                attempt.getTotalMarks() != null ? attempt.getTotalMarks() : 0,
                autoMarked,
                items
        );
    }

    @Transactional
    public void markAnswer(UUID attemptId, UUID answerId, MarkAnswerRequest request, String email) {
        User marker = getUser(email);
        AttemptAnswer answer = answerRepo.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found: " + answerId));

        if (!answer.getAttempt().getId().equals(attemptId)) {
            throw new ForbiddenException("Answer does not belong to this attempt");
        }

        answer.setMarksAwarded(request.marksAwarded());
        answer.setMarkerNote(request.markerNote());
        answer.setMarkedBy(marker);
        answer.setMarkedAt(OffsetDateTime.now());

        // Determine correctness based on marks
        Question q = answer.getQuestion();
        int maxMarks = q.getMarks() != null ? q.getMarks() : 1;
        answer.setIsCorrect(request.marksAwarded() >= maxMarks);

        answerRepo.save(answer);
    }

    @Transactional
    public void finaliseMarking(UUID attemptId, String email) {
        ExamAttempt attempt = findAttemptById(attemptId);
        List<AttemptAnswer> answers = answerRepo.findByAttempt(attempt);

        // Sum all marks
        short totalObtained = 0;
        for (AttemptAnswer answer : answers) {
            if (answer.getMarksAwarded() != null) {
                totalObtained += answer.getMarksAwarded();
            }
        }

        attempt.setMarksObtained(totalObtained);

        if (attempt.getTotalMarks() != null && attempt.getTotalMarks() > 0) {
            BigDecimal pct = BigDecimal.valueOf((double) totalObtained / attempt.getTotalMarks() * 100)
                    .setScale(2, RoundingMode.HALF_UP);
            attempt.setPercentage(pct);

            Short passMark = attempt.getSchedule().getExam().getPassMark();
            if (passMark != null) {
                attempt.setIsPassed(totalObtained >= passMark);
            }
        }

        attemptRepo.save(attempt);
    }

    private boolean needsManualMarking(QuestionType type) {
        return type == QuestionType.SHORT_ANSWER
                || type == QuestionType.ESSAY
                || type == QuestionType.CODE
                || type == QuestionType.IMAGE_BASED;
    }

    private ExamAttempt findAttemptById(UUID id) {
        return attemptRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found: " + id));
    }

    private User getUser(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
