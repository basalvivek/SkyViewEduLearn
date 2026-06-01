package com.edulearn.service;

import com.edulearn.dto.request.ApprovalActionRequest;
import com.edulearn.dto.request.TopicRequest;
import com.edulearn.dto.response.TopicResponse;
import com.edulearn.entity.Subject;
import com.edulearn.entity.Topic;
import com.edulearn.entity.User;
import com.edulearn.enums.ContentStatus;
import com.edulearn.enums.UserRole;
import com.edulearn.exception.ForbiddenException;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.repository.SubjectRepository;
import com.edulearn.repository.TopicRepository;
import com.edulearn.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class TopicService {

    private final TopicRepository topicRepo;
    private final SubjectRepository subjectRepo;
    private final UserRepository userRepo;
    private final ContentApprovalService approvalService;

    public TopicService(TopicRepository topicRepo, SubjectRepository subjectRepo,
                        UserRepository userRepo, ContentApprovalService approvalService) {
        this.topicRepo = topicRepo;
        this.subjectRepo = subjectRepo;
        this.userRepo = userRepo;
        this.approvalService = approvalService;
    }

    public TopicResponse create(String subjectId, TopicRequest request, String actorEmail) {
        User actor = getUser(actorEmail);
        Subject subject = findSubject(subjectId);
        Topic topic = Topic.builder()
            .subject(subject)
            .name(request.name())
            .learningObjective(request.learningObjective())
            .difficulty(request.difficulty())
            .createdBy(actor)
            .status(actor.getRole() == UserRole.ADMIN ? ContentStatus.APPROVED : ContentStatus.DRAFT)
            .build();
        if (actor.getRole() == UserRole.ADMIN) topic.setApprovedBy(actor);
        return toResponse(topicRepo.save(topic));
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> listBySubject(String subjectId, String actorEmail) {
        Subject subject = findSubject(subjectId);
        boolean isAdmin = actorEmail != null && getUser(actorEmail).getRole() == UserRole.ADMIN;
        List<Topic> topics = isAdmin
            ? topicRepo.findBySubject(subject)
            : topicRepo.findBySubjectAndStatus(subject, ContentStatus.APPROVED);
        return topics.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TopicResponse get(String id) {
        return toResponse(findById(id));
    }

    public TopicResponse update(String id, TopicRequest request, String actorEmail) {
        User actor = getUser(actorEmail);
        Topic topic = findById(id);
        checkOwnerOrAdmin(topic.getCreatedBy(), actor);
        topic.setName(request.name());
        topic.setLearningObjective(request.learningObjective());
        topic.setDifficulty(request.difficulty());
        topic.setUpdatedBy(actor);
        if (actor.getRole() != UserRole.ADMIN && topic.getStatus() == ContentStatus.APPROVED) {
            topic.setStatus(ContentStatus.DRAFT);
        }
        return toResponse(topicRepo.save(topic));
    }

    public void delete(String id, String actorEmail) {
        User actor = getUser(actorEmail);
        if (actor.getRole() != UserRole.ADMIN) throw new ForbiddenException("Admin only");
        topicRepo.delete(findById(id));
    }

    public TopicResponse submit(String id, String actorEmail) {
        User actor = getUser(actorEmail);
        Topic topic = findById(id);
        checkOwnerOrAdmin(topic.getCreatedBy(), actor);
        approvalService.submit(topic, actor);
        return toResponse(topicRepo.save(topic));
    }

    public TopicResponse approve(String id, ApprovalActionRequest req, String actorEmail) {
        User admin = getUser(actorEmail);
        if (admin.getRole() != UserRole.ADMIN) throw new ForbiddenException("Admin only");
        Topic topic = findById(id);
        approvalService.approve(topic, admin, req != null ? req.note() : null);
        return toResponse(topicRepo.save(topic));
    }

    public TopicResponse reject(String id, ApprovalActionRequest req, String actorEmail) {
        User admin = getUser(actorEmail);
        if (admin.getRole() != UserRole.ADMIN) throw new ForbiddenException("Admin only");
        Topic topic = findById(id);
        approvalService.reject(topic, admin, req != null ? req.note() : null);
        return toResponse(topicRepo.save(topic));
    }

    @Transactional(readOnly = true)
    public Page<TopicResponse> findByStatus(ContentStatus status, Pageable pageable) {
        return topicRepo.findByStatus(status, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TopicResponse> findByStatusAndCreatedBy(ContentStatus status, User user, Pageable pageable) {
        return topicRepo.findByStatusAndCreatedBy(status, user, pageable).map(this::toResponse);
    }

    private Topic findById(String id) {
        return topicRepo.findById(UUID.fromString(id))
            .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + id));
    }

    private Subject findSubject(String id) {
        return subjectRepo.findById(UUID.fromString(id))
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + id));
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

    public TopicResponse toResponse(Topic topic) {
        return new TopicResponse(
            topic.getId(), topic.getSubject().getId(),
            topic.getName(), topic.getLearningObjective(), topic.getDifficulty(),
            topic.getStatus(), topic.getRejectionReason(),
            topic.getCreatedBy().getFullName(), topic.getCreatedBy().getId(),
            topic.getCreatedAt(), topic.getUpdatedAt());
    }
}
