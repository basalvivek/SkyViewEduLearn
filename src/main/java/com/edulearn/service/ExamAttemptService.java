package com.edulearn.service;

import com.edulearn.dto.request.SaveAnswersRequest;
import com.edulearn.dto.request.StartAttemptRequest;
import com.edulearn.dto.response.AttemptResponse;
import com.edulearn.dto.response.AttemptResultResponse;
import com.edulearn.entity.*;
import com.edulearn.enums.AttemptStatus;
import com.edulearn.enums.QuestionType;
import com.edulearn.enums.ScheduleStatus;
import com.edulearn.exception.ForbiddenException;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExamAttemptService {

    private final ExamAttemptRepository attemptRepo;
    private final AttemptAnswerRepository answerRepo;
    private final ExamScheduleRepository scheduleRepo;
    private final ExamQuestionRepository examQuestionRepo;
    private final QuestionRepository questionRepo;
    private final StudentClassRepository studentClassRepo;
    private final UserRepository userRepo;
    private final AutoMarkingService autoMarkingService;

    public ExamAttemptService(ExamAttemptRepository attemptRepo,
                              AttemptAnswerRepository answerRepo,
                              ExamScheduleRepository scheduleRepo,
                              ExamQuestionRepository examQuestionRepo,
                              QuestionRepository questionRepo,
                              StudentClassRepository studentClassRepo,
                              UserRepository userRepo,
                              AutoMarkingService autoMarkingService) {
        this.attemptRepo = attemptRepo;
        this.answerRepo = answerRepo;
        this.scheduleRepo = scheduleRepo;
        this.examQuestionRepo = examQuestionRepo;
        this.questionRepo = questionRepo;
        this.studentClassRepo = studentClassRepo;
        this.userRepo = userRepo;
        this.autoMarkingService = autoMarkingService;
    }

    @Transactional(readOnly = true)
    public List<AttemptResponse> getAvailableExams(String email) {
        User student = getUser(email);
        OffsetDateTime now = OffsetDateTime.now();

        // Find all active class memberships for the student
        List<Classroom> classrooms = studentClassRepo.findByStudent(student).stream()
                .filter(StudentClass::isActive)
                .map(StudentClass::getClassroom)
                .collect(Collectors.toList());

        List<AttemptResponse> result = new ArrayList<>();
        for (Classroom classroom : classrooms) {
            List<ExamSchedule> schedules = scheduleRepo.findByClassroomOrderByStartAtAsc(classroom);
            for (ExamSchedule schedule : schedules) {
                // Consider schedule active if now is between startAt and endAt
                if (now.isAfter(schedule.getStartAt()) && now.isBefore(schedule.getEndAt())
                        && schedule.getStatus() != ScheduleStatus.CANCELLED) {

                    // Check if student has already completed max attempts
                    long attemptCount = attemptRepo.findBySchedule(schedule).stream()
                            .filter(a -> a.getStudent().getId().equals(student.getId()))
                            .count();
                    if (attemptCount >= schedule.getMaxAttempts()) continue;

                    // Build a lightweight response (no questions needed for listing)
                    result.add(buildAttemptPreview(schedule, student));
                }
            }
        }
        return result;
    }

    public AttemptResponse startAttempt(StartAttemptRequest request, String email) {
        User student = getUser(email);
        ExamSchedule schedule = scheduleRepo.findById(request.scheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + request.scheduleId()));

        // Verify student is in the class
        Classroom classroom = schedule.getClassroom();
        boolean inClass = studentClassRepo.findByClassroomAndIsActiveTrue(classroom).stream()
                .anyMatch(sc -> sc.getStudent().getId().equals(student.getId()));
        if (!inClass) throw new ForbiddenException("Student is not enrolled in this class");

        // Check schedule timing
        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(schedule.getStartAt())) throw new ForbiddenException("Exam has not started yet");
        if (now.isAfter(schedule.getEndAt())) throw new ForbiddenException("Exam has ended");
        if (schedule.getStatus() == ScheduleStatus.CANCELLED) throw new ForbiddenException("Schedule is cancelled");

        // Check attempt count
        long existingAttempts = attemptRepo.findBySchedule(schedule).stream()
                .filter(a -> a.getStudent().getId().equals(student.getId()))
                .count();
        if (existingAttempts >= schedule.getMaxAttempts()) {
            throw new ForbiddenException("Maximum attempts reached");
        }

        // Check for in-progress attempt
        Optional<ExamAttempt> existingInProgress = attemptRepo.findByScheduleAndStudent(schedule, student);
        if (existingInProgress.isPresent() && existingInProgress.get().getStatus() == AttemptStatus.IN_PROGRESS) {
            return buildAttemptResponse(existingInProgress.get());
        }

        // Create new attempt
        Exam exam = schedule.getExam();
        ExamAttempt attempt = ExamAttempt.builder()
                .schedule(schedule)
                .student(student)
                .startedAt(now)
                .status(AttemptStatus.IN_PROGRESS)
                .totalMarks(exam.getTotalMarks())
                .build();
        attempt = attemptRepo.save(attempt);
        return buildAttemptResponse(attempt);
    }

    @Transactional(readOnly = true)
    public AttemptResponse getAttempt(UUID attemptId, String email) {
        User student = getUser(email);
        ExamAttempt attempt = findAttemptById(attemptId);
        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new ForbiddenException("Not your attempt");
        }
        return buildAttemptResponse(attempt);
    }

    public void saveAnswers(UUID attemptId, SaveAnswersRequest request, String email) {
        User student = getUser(email);
        ExamAttempt attempt = findAttemptById(attemptId);
        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new ForbiddenException("Not your attempt");
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ForbiddenException("Attempt is not in progress");
        }

        if (request.answers() == null) return;

        for (SaveAnswersRequest.AnswerItem item : request.answers()) {
            Question question = questionRepo.findById(item.questionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + item.questionId()));

            AttemptAnswer answer = answerRepo.findByAttemptAndQuestion(attempt, question)
                    .orElse(AttemptAnswer.builder().attempt(attempt).question(question).build());

            // Set selected option IDs as JSON string
            if (item.selectedOptionIds() != null && !item.selectedOptionIds().isEmpty()) {
                String json = "[" + item.selectedOptionIds().stream()
                        .map(id -> "\"" + id.toString() + "\"")
                        .collect(Collectors.joining(",")) + "]";
                answer.setSelectedOptionIds(json);
            }
            if (item.booleanAnswer() != null) answer.setBooleanAnswer(item.booleanAnswer());
            if (item.textAnswer() != null) answer.setTextAnswer(item.textAnswer());
            if (item.isFlagged() != null) answer.setFlagged(item.isFlagged());

            answerRepo.save(answer);
        }
    }

    public AttemptResultResponse submitAttempt(UUID attemptId, String email) {
        User student = getUser(email);
        ExamAttempt attempt = findAttemptById(attemptId);
        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new ForbiddenException("Not your attempt");
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ForbiddenException("Attempt is not in progress");
        }

        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(OffsetDateTime.now());
        attempt = attemptRepo.save(attempt);

        // Trigger auto-marking
        autoMarkingService.markAttempt(attempt);

        // Reload attempt after marking
        attempt = findAttemptById(attemptId);
        return buildResultResponse(attempt);
    }

    @Transactional(readOnly = true)
    public AttemptResultResponse getResult(UUID attemptId, String email) {
        User student = getUser(email);
        ExamAttempt attempt = findAttemptById(attemptId);
        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new ForbiddenException("Not your attempt");
        }
        return buildResultResponse(attempt);
    }

    // ---- private helpers ----

    private AttemptResponse buildAttemptPreview(ExamSchedule schedule, User student) {
        return new AttemptResponse(
                null,
                schedule.getId(),
                schedule.getExam().getName(),
                schedule.getExam().getTimeLimitMins(),
                null,
                AttemptStatus.IN_PROGRESS,
                Collections.emptyList()
        );
    }

    private AttemptResponse buildAttemptResponse(ExamAttempt attempt) {
        ExamSchedule schedule = attempt.getSchedule();
        Exam exam = schedule.getExam();
        List<ExamQuestion> examQuestions = examQuestionRepo.findByExamOrderByDisplayOrderAsc(exam);
        List<AttemptAnswer> savedAnswers = answerRepo.findByAttempt(attempt);
        Map<UUID, AttemptAnswer> answerMap = savedAnswers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (a, b) -> a));

        List<AttemptResponse.AttemptQuestionItem> items = examQuestions.stream().map(eq -> {
            Question q = eq.getQuestion();
            AttemptAnswer existing = answerMap.get(q.getId());

            List<AttemptResponse.OptionItem> options = q.getOptions().stream()
                    .map(opt -> new AttemptResponse.OptionItem(opt.getId(), opt.getOptionText()))
                    .collect(Collectors.toList());

            List<UUID> existingSelected = null;
            String existingText = null;
            Boolean existingBool = null;
            boolean isFlagged = false;

            if (existing != null) {
                existingSelected = autoMarkingService.parseSelectedOptionIds(existing.getSelectedOptionIds());
                existingText = existing.getTextAnswer();
                existingBool = existing.getBooleanAnswer();
                isFlagged = existing.isFlagged();
            }

            int marks = eq.getMarksOverride() != null ? eq.getMarksOverride() : (q.getMarks() != null ? q.getMarks() : 1);

            return new AttemptResponse.AttemptQuestionItem(
                    eq.getId(), q.getId(), q.getQuestionText(),
                    q.getQuestionType(), marks, options,
                    q.getCorrectBoolean(),
                    existingText, existingSelected, existingBool, isFlagged
            );
        }).collect(Collectors.toList());

        return new AttemptResponse(
                attempt.getId(), schedule.getId(), exam.getName(),
                exam.getTimeLimitMins(), attempt.getStartedAt(),
                attempt.getStatus(), items
        );
    }

    private AttemptResultResponse buildResultResponse(ExamAttempt attempt) {
        List<AttemptAnswer> answers = answerRepo.findByAttempt(attempt);
        int autoMarked = (int) answers.stream()
                .filter(a -> a.getMarksAwarded() != null || a.getIsCorrect() != null).count();
        int pendingManual = (int) answers.stream()
                .filter(a -> a.getMarksAwarded() == null
                        && needsManualMarking(a.getQuestion().getQuestionType())).count();

        List<AttemptResultResponse.AnswerResult> answerResults = answers.stream().map(a -> {
            Question q = a.getQuestion();
            int maxMarks = q.getMarks() != null ? q.getMarks() : 1;
            return new AttemptResultResponse.AnswerResult(
                    q.getId(), q.getQuestionText(), q.getQuestionType(), maxMarks,
                    a.getMarksAwarded() != null ? (int) a.getMarksAwarded() : null,
                    a.getIsCorrect(), a.getMarkerNote()
            );
        }).collect(Collectors.toList());

        return new AttemptResultResponse(
                attempt.getId(),
                attempt.getSchedule().getExam().getName(),
                attempt.getTotalMarks() != null ? attempt.getTotalMarks() : 0,
                attempt.getMarksObtained() != null ? (int) attempt.getMarksObtained() : null,
                attempt.getPercentage(),
                attempt.getIsPassed(),
                attempt.getStatus(),
                autoMarked,
                pendingManual,
                answerResults
        );
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
