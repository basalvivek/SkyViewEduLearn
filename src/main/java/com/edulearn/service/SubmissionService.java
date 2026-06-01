package com.edulearn.service;

import com.edulearn.dto.response.SubmissionResponse;
import com.edulearn.dto.response.SubmissionSummaryResponse;
import com.edulearn.entity.*;
import com.edulearn.enums.ContentStatus;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubmissionService {

    private final UserRepository userRepo;
    private final CategoryRepository categoryRepo;
    private final SubjectRepository subjectRepo;
    private final TopicRepository topicRepo;
    private final QuestionRepository questionRepo;

    public SubmissionService(UserRepository userRepo, CategoryRepository categoryRepo,
                              SubjectRepository subjectRepo, TopicRepository topicRepo,
                              QuestionRepository questionRepo) {
        this.userRepo = userRepo;
        this.categoryRepo = categoryRepo;
        this.subjectRepo = subjectRepo;
        this.topicRepo = topicRepo;
        this.questionRepo = questionRepo;
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissions(String email, ContentStatus status, String type,
                                                    int page, int size) {
        User user = findByEmail(email);
        List<SubmissionResponse> all = new ArrayList<>();

        if (type == null || type.equalsIgnoreCase("CATEGORY")) {
            List<Category> cats = status == null
                    ? categoryRepo.findByCreatedByOrderByUpdatedAtDesc(user)
                    : categoryRepo.findByStatusAndCreatedByOrderByUpdatedAtDesc(status, user);
            cats.forEach(c -> all.add(fromCategory(c)));
        }
        if (type == null || type.equalsIgnoreCase("SUBJECT")) {
            List<Subject> subs = status == null
                    ? subjectRepo.findByCreatedByOrderByUpdatedAtDesc(user)
                    : subjectRepo.findByStatusAndCreatedByOrderByUpdatedAtDesc(status, user);
            subs.forEach(s -> all.add(fromSubject(s)));
        }
        if (type == null || type.equalsIgnoreCase("TOPIC")) {
            List<Topic> topics = status == null
                    ? topicRepo.findByCreatedByOrderByUpdatedAtDesc(user)
                    : topicRepo.findByStatusAndCreatedByOrderByUpdatedAtDesc(status, user);
            topics.forEach(t -> all.add(fromTopic(t)));
        }
        if (type == null || type.equalsIgnoreCase("QUESTION")) {
            List<Question> qs = status == null
                    ? questionRepo.findByCreatedByOrderByUpdatedAtDesc(user)
                    : questionRepo.findByStatusAndCreatedByOrderByUpdatedAtDesc(status, user);
            qs.forEach(q -> all.add(fromQuestion(q)));
        }

        all.sort(Comparator.comparing(
                s -> s.getSubmittedAt() != null ? s.getSubmittedAt() : java.time.OffsetDateTime.MIN,
                Comparator.reverseOrder()));

        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        return all.subList(from, to);
    }

    public long countSubmissions(String email, ContentStatus status, String type) {
        User user = findByEmail(email);
        long count = 0;
        if (type == null || type.equalsIgnoreCase("CATEGORY"))
            count += status == null ? categoryRepo.countByCreatedBy(user) : categoryRepo.countByStatusAndCreatedBy(status, user);
        if (type == null || type.equalsIgnoreCase("SUBJECT"))
            count += status == null ? subjectRepo.countByCreatedBy(user) : subjectRepo.countByStatusAndCreatedBy(status, user);
        if (type == null || type.equalsIgnoreCase("TOPIC"))
            count += status == null ? topicRepo.countByCreatedBy(user) : topicRepo.countByStatusAndCreatedBy(status, user);
        if (type == null || type.equalsIgnoreCase("QUESTION"))
            count += status == null ? questionRepo.countByCreatedBy(user) : questionRepo.countByStatusAndCreatedBy(status, user);
        return count;
    }

    public SubmissionSummaryResponse getSummary(String email) {
        User user = findByEmail(email);
        long draft = sum(categoryRepo.countByStatusAndCreatedBy(ContentStatus.DRAFT, user),
                subjectRepo.countByStatusAndCreatedBy(ContentStatus.DRAFT, user),
                topicRepo.countByStatusAndCreatedBy(ContentStatus.DRAFT, user),
                questionRepo.countByStatusAndCreatedBy(ContentStatus.DRAFT, user));
        long pending = sum(categoryRepo.countByStatusAndCreatedBy(ContentStatus.PENDING, user),
                subjectRepo.countByStatusAndCreatedBy(ContentStatus.PENDING, user),
                topicRepo.countByStatusAndCreatedBy(ContentStatus.PENDING, user),
                questionRepo.countByStatusAndCreatedBy(ContentStatus.PENDING, user));
        long approved = sum(categoryRepo.countByStatusAndCreatedBy(ContentStatus.APPROVED, user),
                subjectRepo.countByStatusAndCreatedBy(ContentStatus.APPROVED, user),
                topicRepo.countByStatusAndCreatedBy(ContentStatus.APPROVED, user),
                questionRepo.countByStatusAndCreatedBy(ContentStatus.APPROVED, user));
        long rejected = sum(categoryRepo.countByStatusAndCreatedBy(ContentStatus.REJECTED, user),
                subjectRepo.countByStatusAndCreatedBy(ContentStatus.REJECTED, user),
                topicRepo.countByStatusAndCreatedBy(ContentStatus.REJECTED, user),
                questionRepo.countByStatusAndCreatedBy(ContentStatus.REJECTED, user));
        return new SubmissionSummaryResponse(draft, pending, approved, rejected);
    }

    private long sum(long... vals) { long t = 0; for (long v : vals) t += v; return t; }

    private SubmissionResponse fromCategory(Category c) {
        SubmissionResponse r = new SubmissionResponse();
        r.setId(UUID.randomUUID()); r.setEntityId(c.getId()); r.setEntityType("CATEGORY");
        r.setName(c.getName()); r.setPath("Category");
        r.setStatus(c.getStatus()); r.setRejectionReason(c.getRejectionReason());
        r.setSubmittedAt(c.getUpdatedAt());
        if (c.getApprovedBy() != null) { r.setReviewedAt(c.getApprovedAt()); r.setReviewedByName(c.getApprovedBy().getFullName()); }
        return r;
    }

    private SubmissionResponse fromSubject(Subject s) {
        SubmissionResponse r = new SubmissionResponse();
        r.setId(UUID.randomUUID()); r.setEntityId(s.getId()); r.setEntityType("SUBJECT");
        r.setName(s.getName());
        r.setPath(s.getCategory() != null ? s.getCategory().getName() : "");
        r.setStatus(s.getStatus()); r.setRejectionReason(s.getRejectionReason());
        r.setSubmittedAt(s.getUpdatedAt());
        if (s.getApprovedBy() != null) { r.setReviewedAt(s.getApprovedAt()); r.setReviewedByName(s.getApprovedBy().getFullName()); }
        return r;
    }

    private SubmissionResponse fromTopic(Topic t) {
        SubmissionResponse r = new SubmissionResponse();
        r.setId(UUID.randomUUID()); r.setEntityId(t.getId()); r.setEntityType("TOPIC");
        r.setName(t.getName());
        String path = "";
        if (t.getSubject() != null) {
            path = t.getSubject().getName();
            if (t.getSubject().getCategory() != null) path = t.getSubject().getCategory().getName() + " › " + path;
        }
        r.setPath(path);
        r.setStatus(t.getStatus()); r.setRejectionReason(t.getRejectionReason());
        r.setSubmittedAt(t.getUpdatedAt());
        if (t.getApprovedBy() != null) { r.setReviewedAt(t.getApprovedAt()); r.setReviewedByName(t.getApprovedBy().getFullName()); }
        return r;
    }

    private SubmissionResponse fromQuestion(Question q) {
        SubmissionResponse r = new SubmissionResponse();
        r.setId(UUID.randomUUID()); r.setEntityId(q.getId()); r.setEntityType("QUESTION");
        r.setName(q.getQuestionText() != null ? q.getQuestionText().substring(0, Math.min(80, q.getQuestionText().length())) : "");
        String path = "";
        if (q.getTopic() != null) {
            path = q.getTopic().getName();
            if (q.getTopic().getSubject() != null) {
                path = q.getTopic().getSubject().getName() + " › " + path;
                if (q.getTopic().getSubject().getCategory() != null)
                    path = q.getTopic().getSubject().getCategory().getName() + " › " + path;
            }
        }
        r.setPath(path);
        r.setStatus(q.getStatus()); r.setRejectionReason(q.getRejectionReason());
        r.setSubmittedAt(q.getUpdatedAt());
        if (q.getApprovedBy() != null) { r.setReviewedAt(q.getApprovedAt()); r.setReviewedByName(q.getApprovedBy().getFullName()); }
        return r;
    }

    private User findByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
