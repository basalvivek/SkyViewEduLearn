package com.edulearn.controller;

import com.edulearn.dto.response.SubmissionResponse;
import com.edulearn.entity.User;
import com.edulearn.enums.ContentStatus;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.repository.*;
import com.edulearn.util.ApiResponse;
import com.edulearn.util.PageMeta;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Transactional(readOnly = true)
public class ApprovalController {

    private final CategoryRepository categoryRepo;
    private final SubjectRepository subjectRepo;
    private final TopicRepository topicRepo;
    private final QuestionRepository questionRepo;
    private final UserRepository userRepo;

    public ApprovalController(CategoryRepository categoryRepo, SubjectRepository subjectRepo,
                               TopicRepository topicRepo, QuestionRepository questionRepo,
                               UserRepository userRepo) {
        this.categoryRepo = categoryRepo;
        this.subjectRepo = subjectRepo;
        this.topicRepo = topicRepo;
        this.questionRepo = questionRepo;
        this.userRepo = userRepo;
    }

    @GetMapping("/approvals/pending")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> pending(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        List<SubmissionResponse> items = new ArrayList<>();

        if (type == null || "CATEGORY".equalsIgnoreCase(type)) {
            categoryRepo.findByStatus(ContentStatus.PENDING, pageable).forEach(c ->
                items.add(new SubmissionResponse(c.getId(), "CATEGORY", c.getName(),
                    c.getStatus(), c.getRejectionReason(), c.getName(),
                    c.getCreatedBy().getFullName(), c.getCreatedAt(), c.getUpdatedAt())));
        }
        if (type == null || "SUBJECT".equalsIgnoreCase(type)) {
            subjectRepo.findByStatus(ContentStatus.PENDING, pageable).forEach(s ->
                items.add(new SubmissionResponse(s.getId(), "SUBJECT", s.getName(),
                    s.getStatus(), s.getRejectionReason(), s.getCategory().getName() + " > " + s.getName(),
                    s.getCreatedBy().getFullName(), s.getCreatedAt(), s.getUpdatedAt())));
        }
        if (type == null || "TOPIC".equalsIgnoreCase(type)) {
            topicRepo.findByStatus(ContentStatus.PENDING, pageable).forEach(t ->
                items.add(new SubmissionResponse(t.getId(), "TOPIC", t.getName(),
                    t.getStatus(), t.getRejectionReason(),
                    t.getSubject().getCategory().getName() + " > " + t.getSubject().getName() + " > " + t.getName(),
                    t.getCreatedBy().getFullName(), t.getCreatedAt(), t.getUpdatedAt())));
        }
        if (type == null || "QUESTION".equalsIgnoreCase(type)) {
            questionRepo.findByStatus(ContentStatus.PENDING, pageable).forEach(q ->
                items.add(new SubmissionResponse(q.getId(), "QUESTION", q.getQuestionText(),
                    q.getStatus(), q.getRejectionReason(),
                    q.getTopic().getSubject().getCategory().getName() + " > " + q.getTopic().getSubject().getName() + " > " + q.getTopic().getName(),
                    q.getCreatedBy().getFullName(), q.getCreatedAt(), q.getUpdatedAt())));
        }

        return ResponseEntity.ok(new ApiResponse<>("success", "OK", items,
            new PageMeta(page, size, items.size(), 1)));
    }

}
