package com.edulearn.service;

import com.edulearn.dto.request.ApprovalActionRequest;
import com.edulearn.dto.request.OptionRequest;
import com.edulearn.dto.request.QuestionRequest;
import com.edulearn.dto.response.QuestionOptionResponse;
import com.edulearn.dto.response.QuestionResponse;
import com.edulearn.entity.Question;
import com.edulearn.entity.QuestionOption;
import com.edulearn.entity.Topic;
import com.edulearn.entity.User;
import com.edulearn.enums.ContentStatus;
import com.edulearn.enums.QuestionType;
import com.edulearn.enums.UserRole;
import com.edulearn.exception.ForbiddenException;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.exception.ValidationException;
import com.edulearn.repository.QuestionRepository;
import com.edulearn.repository.TopicRepository;
import com.edulearn.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuestionService {

    private final QuestionRepository questionRepo;
    private final TopicRepository topicRepo;
    private final UserRepository userRepo;
    private final ContentApprovalService approvalService;

    public QuestionService(QuestionRepository questionRepo, TopicRepository topicRepo,
                           UserRepository userRepo, ContentApprovalService approvalService) {
        this.questionRepo = questionRepo;
        this.topicRepo = topicRepo;
        this.userRepo = userRepo;
        this.approvalService = approvalService;
    }

    public QuestionResponse create(String topicId, QuestionRequest request, String actorEmail) {
        User actor = getUser(actorEmail);
        Topic topic = findTopic(topicId);
        validateMcqOptions(request);
        if (request.questionType() == QuestionType.IMAGE_BASED && request.imageAnswerType() == null)
            throw new ValidationException("IMAGE_BASED questions must have imageAnswerType set");

        Question q = Question.builder()
            .topic(topic)
            .questionText(request.questionText())
            .questionType(request.questionType())
            .marks(request.marks() != null ? request.marks() : 1)
            .answerExplanation(request.answerExplanation())
            .correctBoolean(request.correctBoolean())
            .modelAnswer(request.modelAnswer())
            .markingScheme(request.markingScheme())
            .wordLimit(request.wordLimit())
            .codeLang(request.codeLang())
            .starterCode(request.starterCode())
            .expectedOutput(request.expectedOutput())
            .imageUrl(request.imageUrl())
            .imageAltText(request.imageAltText())
            .imageAnswerType(request.imageAnswerType())
            .createdBy(actor)
            .status(actor.getRole() == UserRole.ADMIN ? ContentStatus.APPROVED : ContentStatus.DRAFT)
            .build();
        if (actor.getRole() == UserRole.ADMIN) q.setApprovedBy(actor);

        if (request.options() != null) {
            List<QuestionOption> opts = new ArrayList<>();
            for (int i = 0; i < request.options().size(); i++) {
                var opt = request.options().get(i);
                opts.add(QuestionOption.builder()
                    .question(q)
                    .optionText(opt.optionText())
                    .isCorrect(opt.isCorrect())
                    .displayOrder((short) opt.displayOrder())
                    .partialMarks(opt.partialMarks())
                    .build());
            }
            q.setOptions(opts);
        }
        return toResponse(questionRepo.save(q));
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> listByTopic(String topicId, String actorEmail) {
        Topic topic = findTopic(topicId);
        boolean isAdmin = actorEmail != null && getUser(actorEmail).getRole() == UserRole.ADMIN;
        List<Question> questions = isAdmin
            ? questionRepo.findByTopic(topic)
            : questionRepo.findByTopicAndStatus(topic, ContentStatus.APPROVED);
        return questions.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuestionResponse get(String id) {
        return toResponse(findById(id));
    }

    public QuestionResponse update(String id, QuestionRequest request, String actorEmail) {
        User actor = getUser(actorEmail);
        Question q = findById(id);
        checkOwnerOrAdmin(q.getCreatedBy(), actor);
        validateMcqOptions(request);
        if (request.questionType() == QuestionType.IMAGE_BASED && request.imageAnswerType() == null)
            throw new ValidationException("IMAGE_BASED questions must have imageAnswerType set");

        q.setQuestionText(request.questionText());
        q.setQuestionType(request.questionType());
        if (request.marks() != null) q.setMarks(request.marks());
        q.setAnswerExplanation(request.answerExplanation());
        q.setCorrectBoolean(request.correctBoolean());
        q.setModelAnswer(request.modelAnswer());
        q.setMarkingScheme(request.markingScheme());
        q.setWordLimit(request.wordLimit());
        q.setCodeLang(request.codeLang());
        q.setStarterCode(request.starterCode());
        q.setExpectedOutput(request.expectedOutput());
        q.setImageUrl(request.imageUrl());
        q.setImageAltText(request.imageAltText());
        q.setImageAnswerType(request.imageAnswerType());
        q.setUpdatedBy(actor);
        if (actor.getRole() != UserRole.ADMIN && q.getStatus() == ContentStatus.APPROVED) {
            q.setStatus(ContentStatus.DRAFT);
        }
        if (request.options() != null) {
            q.getOptions().clear();
            for (var opt : request.options()) {
                q.getOptions().add(QuestionOption.builder()
                    .question(q)
                    .optionText(opt.optionText())
                    .isCorrect(opt.isCorrect())
                    .displayOrder((short) opt.displayOrder())
                    .partialMarks(opt.partialMarks())
                    .build());
            }
        }
        return toResponse(questionRepo.save(q));
    }

    public void delete(String id, String actorEmail) {
        User actor = getUser(actorEmail);
        if (actor.getRole() != UserRole.ADMIN) throw new ForbiddenException("Admin only");
        questionRepo.delete(findById(id));
    }

    public QuestionResponse submit(String id, String actorEmail) {
        User actor = getUser(actorEmail);
        Question q = findById(id);
        checkOwnerOrAdmin(q.getCreatedBy(), actor);
        approvalService.submit(q, actor);
        return toResponse(questionRepo.save(q));
    }

    public QuestionResponse approve(String id, ApprovalActionRequest req, String actorEmail) {
        User admin = getUser(actorEmail);
        if (admin.getRole() != UserRole.ADMIN) throw new ForbiddenException("Admin only");
        Question q = findById(id);
        approvalService.approve(q, admin, req != null ? req.note() : null);
        return toResponse(questionRepo.save(q));
    }

    public QuestionResponse reject(String id, ApprovalActionRequest req, String actorEmail) {
        User admin = getUser(actorEmail);
        if (admin.getRole() != UserRole.ADMIN) throw new ForbiddenException("Admin only");
        Question q = findById(id);
        approvalService.reject(q, admin, req != null ? req.note() : null);
        return toResponse(questionRepo.save(q));
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponse> searchQuestions(ContentStatus status, UUID topicId, UUID subjectId,
            UUID categoryId, String type, String difficulty, int page, int size) {
        final QuestionType qType;
        if (type != null && !type.isBlank()) {
            QuestionType parsed = null;
            try { parsed = QuestionType.valueOf(type); } catch (IllegalArgumentException ignored) {}
            qType = parsed;
        } else {
            qType = null;
        }
        Specification<Question> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), status));
            if (topicId != null)
                predicates.add(cb.equal(root.get("topic").get("id"), topicId));
            if (subjectId != null)
                predicates.add(cb.equal(root.get("topic").get("subject").get("id"), subjectId));
            if (categoryId != null)
                predicates.add(cb.equal(root.get("topic").get("subject").get("category").get("id"), categoryId));
            if (qType != null)
                predicates.add(cb.equal(root.get("questionType"), qType));
            if (difficulty != null && !difficulty.isBlank())
                predicates.add(cb.equal(cb.lower(root.get("topic").get("difficulty")), difficulty.toLowerCase()));
            if (query != null) query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return questionRepo.findAll(spec, PageRequest.of(page, size)).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponse> findByStatus(ContentStatus status, Pageable pageable) {
        return questionRepo.findByStatus(status, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponse> findByStatusAndCreatedBy(ContentStatus status, User user, Pageable pageable) {
        return questionRepo.findByStatusAndCreatedBy(status, user, pageable).map(this::toResponse);
    }

    private Question findById(String id) {
        return questionRepo.findById(UUID.fromString(id))
            .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + id));
    }

    private Topic findTopic(String id) {
        return topicRepo.findById(UUID.fromString(id))
            .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + id));
    }

    private User getUser(String email) {
        return userRepo.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private void checkOwnerOrAdmin(User owner, User actor) {
        if (actor.getRole() != UserRole.ADMIN && !owner.getId().equals(actor.getId())) {
            throw new ForbiddenException("You don't have permission to modify this resource");
        }
    }

    private void validateMcqOptions(QuestionRequest request) {
        QuestionType qt = request.questionType();
        if (qt != QuestionType.MCQ_SINGLE && qt != QuestionType.MCQ_MULTIPLE) return;
        if (request.options() == null || request.options().size() < 2)
            throw new ValidationException("MCQ questions must have at least 2 options");
        long correctCount = request.options().stream().filter(OptionRequest::isCorrect).count();
        if (qt == QuestionType.MCQ_SINGLE && correctCount != 1)
            throw new ValidationException("MCQ_SINGLE must have exactly 1 correct option");
        if (qt == QuestionType.MCQ_MULTIPLE && correctCount < 1)
            throw new ValidationException("MCQ_MULTIPLE must have at least 1 correct option");
    }

    public QuestionResponse toResponse(Question q) {
        List<QuestionOptionResponse> opts = q.getOptions().stream()
            .map(o -> new QuestionOptionResponse(o.getId(), o.getOptionText(), o.isCorrect(), o.getDisplayOrder(), o.getPartialMarks()))
            .collect(Collectors.toList());
        return new QuestionResponse(
            q.getId(), q.getTopic().getId(),
            q.getQuestionText(), q.getQuestionType(), q.getMarks(), q.getAnswerExplanation(), opts,
            q.getCorrectBoolean(), q.getModelAnswer(), q.getMarkingScheme(), q.getWordLimit(),
            q.getCodeLang(), q.getStarterCode(), q.getExpectedOutput(),
            q.getImageUrl(), q.getImageAltText(), q.getImageAnswerType(),
            q.getStatus(), q.getRejectionReason(),
            q.getCreatedBy().getFullName(), q.getCreatedBy().getId(),
            q.getCreatedAt(), q.getUpdatedAt());
    }
}
