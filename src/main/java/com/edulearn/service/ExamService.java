package com.edulearn.service;

import com.edulearn.dto.request.ApprovalActionRequest;
import com.edulearn.dto.request.ExamQuestionsRequest;
import com.edulearn.dto.request.ExamRequest;
import com.edulearn.dto.response.ExamResponse;
import com.edulearn.dto.response.ExamSummaryResponse;
import com.edulearn.entity.Exam;
import com.edulearn.entity.ExamQuestion;
import com.edulearn.entity.Question;
import com.edulearn.entity.User;
import com.edulearn.enums.ExamStatus;
import com.edulearn.enums.UserRole;
import com.edulearn.exception.ForbiddenException;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.repository.ExamQuestionRepository;
import com.edulearn.repository.ExamRepository;
import com.edulearn.repository.QuestionRepository;
import com.edulearn.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExamService {

    private final ExamRepository examRepo;
    private final ExamQuestionRepository examQuestionRepo;
    private final QuestionRepository questionRepo;
    private final UserRepository userRepo;

    public ExamService(ExamRepository examRepo,
                       ExamQuestionRepository examQuestionRepo,
                       QuestionRepository questionRepo,
                       UserRepository userRepo) {
        this.examRepo = examRepo;
        this.examQuestionRepo = examQuestionRepo;
        this.questionRepo = questionRepo;
        this.userRepo = userRepo;
    }

    public ExamResponse createExam(ExamRequest request, String email) {
        User actor = getUser(email);
        Exam exam = Exam.builder()
                .name(request.name())
                .description(request.description())
                .timeLimitMins((short) request.timeLimitMins())
                .passMark(request.passMark() != null ? request.passMark().shortValue() : null)
                .shuffleQuestions(request.shuffleQuestions())
                .shuffleOptions(request.shuffleOptions())
                .createdBy(actor)
                .status(actor.getRole() == UserRole.ADMIN ? ExamStatus.APPROVED : ExamStatus.DRAFT)
                .build();
        if (actor.getRole() == UserRole.ADMIN) {
            exam.setApprovedBy(actor);
            exam.setApprovedAt(OffsetDateTime.now());
        }
        exam = examRepo.save(exam);
        if (request.questionIds() != null && !request.questionIds().isEmpty()) {
            exam = setQuestionsInternal(exam, request.questionIds());
        }
        return toResponse(exam);
    }

    @Transactional(readOnly = true)
    public ExamResponse getExam(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<ExamSummaryResponse> listExams(String email) {
        User actor = getUser(email);
        List<Exam> exams;
        if (actor.getRole() == UserRole.ADMIN) {
            exams = examRepo.findAll();
        } else {
            exams = examRepo.findByCreatedByOrderByCreatedAtDesc(actor);
        }
        return exams.stream().map(this::toSummary).collect(Collectors.toList());
    }

    public ExamResponse updateExam(UUID id, ExamRequest request, String email) {
        User actor = getUser(email);
        Exam exam = findById(id);
        checkOwnerOrAdmin(exam.getCreatedBy(), actor);
        exam.setName(request.name());
        exam.setDescription(request.description());
        exam.setTimeLimitMins((short) request.timeLimitMins());
        exam.setPassMark(request.passMark() != null ? request.passMark().shortValue() : null);
        exam.setShuffleQuestions(request.shuffleQuestions());
        exam.setShuffleOptions(request.shuffleOptions());
        exam.setUpdatedBy(actor);
        if (actor.getRole() != UserRole.ADMIN && exam.getStatus() == ExamStatus.APPROVED) {
            exam.setStatus(ExamStatus.DRAFT);
        }
        if (request.questionIds() != null) {
            exam = setQuestionsInternal(exam, request.questionIds());
        }
        return toResponse(examRepo.save(exam));
    }

    public ExamResponse setQuestions(UUID examId, ExamQuestionsRequest request, String email) {
        User actor = getUser(email);
        Exam exam = findById(examId);
        checkOwnerOrAdmin(exam.getCreatedBy(), actor);
        exam = setQuestionsInternal(exam, request.questionIds());
        return toResponse(exam);
    }

    public void deleteExam(UUID id, String email) {
        User actor = getUser(email);
        if (actor.getRole() != UserRole.ADMIN) throw new ForbiddenException("Admin only");
        Exam exam = findById(id);
        examRepo.delete(exam);
    }

    public ExamResponse submitExam(UUID id, String email) {
        User actor = getUser(email);
        Exam exam = findById(id);
        checkOwnerOrAdmin(exam.getCreatedBy(), actor);
        if (exam.getStatus() != ExamStatus.DRAFT && exam.getStatus() != ExamStatus.REJECTED) {
            throw new IllegalStateException("Can only submit DRAFT or REJECTED exams");
        }
        exam.setStatus(ExamStatus.PENDING);
        exam.setUpdatedBy(actor);
        return toResponse(examRepo.save(exam));
    }

    public ExamResponse approveExam(UUID id, String email, String note) {
        User admin = getUser(email);
        if (admin.getRole() != UserRole.ADMIN) throw new ForbiddenException("Admin only");
        Exam exam = findById(id);
        exam.setStatus(ExamStatus.APPROVED);
        exam.setApprovedBy(admin);
        exam.setApprovedAt(OffsetDateTime.now());
        exam.setRejectionReason(null);
        return toResponse(examRepo.save(exam));
    }

    public ExamResponse rejectExam(UUID id, String email, String note) {
        User admin = getUser(email);
        if (admin.getRole() != UserRole.ADMIN) throw new ForbiddenException("Admin only");
        Exam exam = findById(id);
        exam.setStatus(ExamStatus.REJECTED);
        exam.setRejectionReason(note);
        return toResponse(examRepo.save(exam));
    }

    // ---- internal helpers ----

    private Exam setQuestionsInternal(Exam exam, List<UUID> questionIds) {
        examQuestionRepo.deleteByExam(exam);
        List<ExamQuestion> eqs = new ArrayList<>();
        short total = 0;
        for (int i = 0; i < questionIds.size(); i++) {
            UUID qid = questionIds.get(i);
            Question q = questionRepo.findById(qid)
                    .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + qid));
            ExamQuestion eq = ExamQuestion.builder()
                    .exam(exam)
                    .question(q)
                    .displayOrder((short) (i + 1))
                    .build();
            eqs.add(eq);
            total += q.getMarks() != null ? q.getMarks() : 1;
        }
        examQuestionRepo.saveAll(eqs);
        exam.setTotalMarks(total);
        return examRepo.save(exam);
    }

    public ExamResponse toResponse(Exam exam) {
        List<ExamQuestion> eqs = examQuestionRepo.findByExamOrderByDisplayOrderAsc(exam);
        List<ExamResponse.ExamQuestionItem> items = eqs.stream().map(eq -> {
            Question q = eq.getQuestion();
            int marks = eq.getMarksOverride() != null
                    ? eq.getMarksOverride()
                    : (q.getMarks() != null ? q.getMarks() : 1);
            return new ExamResponse.ExamQuestionItem(
                    eq.getId(),
                    q.getId(),
                    q.getQuestionText(),
                    q.getQuestionType().name(),
                    marks,
                    eq.getDisplayOrder()
            );
        }).collect(Collectors.toList());

        return new ExamResponse(
                exam.getId(),
                exam.getName(),
                exam.getDescription(),
                exam.getTimeLimitMins(),
                exam.getTotalMarks(),
                exam.getPassMark() != null ? (int) exam.getPassMark() : null,
                exam.isShuffleQuestions(),
                exam.isShuffleOptions(),
                exam.getStatus(),
                exam.getRejectionReason(),
                exam.getCreatedBy().getFullName(),
                items.size(),
                items
        );
    }

    private ExamSummaryResponse toSummary(Exam exam) {
        List<ExamQuestion> eqs = examQuestionRepo.findByExamOrderByDisplayOrderAsc(exam);
        return new ExamSummaryResponse(
                exam.getId(),
                exam.getName(),
                exam.getStatus(),
                exam.getTimeLimitMins(),
                exam.getTotalMarks(),
                eqs.size(),
                exam.getCreatedBy().getFullName(),
                exam.getCreatedAt()
        );
    }

    private Exam findById(UUID id) {
        return examRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + id));
    }

    private User getUser(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private void checkOwnerOrAdmin(User owner, User actor) {
        if (actor.getRole() != UserRole.ADMIN && !owner.getId().equals(actor.getId())) {
            throw new ForbiddenException("You don't have permission to modify this exam");
        }
    }
}
