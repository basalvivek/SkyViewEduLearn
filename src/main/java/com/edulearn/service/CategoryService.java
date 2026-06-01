package com.edulearn.service;

import com.edulearn.dto.request.ApprovalActionRequest;
import com.edulearn.dto.request.CategoryRequest;
import com.edulearn.dto.response.CategoryResponse;
import com.edulearn.dto.response.SubjectResponse;
import com.edulearn.dto.response.TopicResponse;
import com.edulearn.dto.response.TreeNodeResponse;
import com.edulearn.entity.Category;
import com.edulearn.entity.Subject;
import com.edulearn.entity.Topic;
import com.edulearn.entity.User;
import com.edulearn.enums.ContentStatus;
import com.edulearn.enums.UserRole;
import com.edulearn.exception.ForbiddenException;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.repository.CategoryRepository;
import com.edulearn.repository.QuestionRepository;
import com.edulearn.repository.SubjectRepository;
import com.edulearn.repository.TopicRepository;
import com.edulearn.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepo;
    private final SubjectRepository subjectRepo;
    private final TopicRepository topicRepo;
    private final QuestionRepository questionRepo;
    private final UserRepository userRepo;
    private final ContentApprovalService approvalService;

    public CategoryService(CategoryRepository categoryRepo, SubjectRepository subjectRepo,
                           TopicRepository topicRepo, QuestionRepository questionRepo,
                           UserRepository userRepo, ContentApprovalService approvalService) {
        this.categoryRepo = categoryRepo;
        this.subjectRepo = subjectRepo;
        this.topicRepo = topicRepo;
        this.questionRepo = questionRepo;
        this.userRepo = userRepo;
        this.approvalService = approvalService;
    }

    public CategoryResponse create(CategoryRequest request, String actorEmail) {
        User actor = getUser(actorEmail);
        Category cat = Category.builder()
            .name(request.name())
            .description(request.description())
            .createdBy(actor)
            .status(actor.getRole() == UserRole.ADMIN ? ContentStatus.APPROVED : ContentStatus.DRAFT)
            .build();
        if (actor.getRole() == UserRole.ADMIN) {
            cat.setApprovedBy(actor);
        }
        return toResponse(categoryRepo.save(cat));
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> list(String actorEmail, Pageable pageable) {
        User actor = getUser(actorEmail);
        if (actor.getRole() == UserRole.ADMIN) {
            return categoryRepo.findAll(pageable).map(this::toResponse);
        }
        return categoryRepo.findByStatus(ContentStatus.APPROVED, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> listPublic(Pageable pageable) {
        return categoryRepo.findByStatus(ContentStatus.APPROVED, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(String id) {
        return toResponse(findById(id));
    }

    public CategoryResponse update(String id, CategoryRequest request, String actorEmail) {
        User actor = getUser(actorEmail);
        Category cat = findById(id);
        checkOwnerOrAdmin(cat.getCreatedBy(), actor);
        cat.setName(request.name());
        cat.setDescription(request.description());
        cat.setUpdatedBy(actor);
        if (actor.getRole() != UserRole.ADMIN && cat.getStatus() == ContentStatus.APPROVED) {
            cat.setStatus(ContentStatus.DRAFT);
        }
        return toResponse(categoryRepo.save(cat));
    }

    public void delete(String id, String actorEmail) {
        User actor = getUser(actorEmail);
        if (actor.getRole() != UserRole.ADMIN) throw new ForbiddenException("Admin only");
        Category cat = findById(id);
        categoryRepo.delete(cat);
    }

    public CategoryResponse submit(String id, String actorEmail) {
        User actor = getUser(actorEmail);
        Category cat = findById(id);
        checkOwnerOrAdmin(cat.getCreatedBy(), actor);
        approvalService.submit(cat, actor);
        return toResponse(categoryRepo.save(cat));
    }

    public CategoryResponse approve(String id, ApprovalActionRequest req, String actorEmail) {
        User admin = getUser(actorEmail);
        if (admin.getRole() != UserRole.ADMIN) throw new ForbiddenException("Admin only");
        Category cat = findById(id);
        approvalService.approve(cat, admin, req != null ? req.note() : null);
        return toResponse(categoryRepo.save(cat));
    }

    public CategoryResponse reject(String id, ApprovalActionRequest req, String actorEmail) {
        User admin = getUser(actorEmail);
        if (admin.getRole() != UserRole.ADMIN) throw new ForbiddenException("Admin only");
        Category cat = findById(id);
        approvalService.reject(cat, admin, req != null ? req.note() : null);
        return toResponse(categoryRepo.save(cat));
    }

    @Transactional(readOnly = true)
    public TreeNodeResponse getTree(String id, String actorEmail) {
        Category cat = findById(id);
        boolean isAdmin = actorEmail != null && getUser(actorEmail).getRole() == UserRole.ADMIN;
        return buildCategoryNode(cat, isAdmin);
    }

    private TreeNodeResponse buildCategoryNode(Category cat, boolean isAdmin) {
        List<Subject> subjects = isAdmin
            ? subjectRepo.findByCategory(cat)
            : subjectRepo.findByCategoryAndStatus(cat, ContentStatus.APPROVED);

        List<TreeNodeResponse> children = subjects.stream()
            .map(s -> buildSubjectNode(s, isAdmin))
            .collect(Collectors.toList());

        return new TreeNodeResponse(
            cat.getId(), cat.getName(), "category", cat.getStatus(),
            cat.getRejectionReason(), cat.getCreatedBy().getFullName(),
            cat.getCreatedBy().getId(), children);
    }

    private TreeNodeResponse buildSubjectNode(Subject sub, boolean isAdmin) {
        List<Topic> topics = isAdmin
            ? topicRepo.findBySubject(sub)
            : topicRepo.findBySubjectAndStatus(sub, ContentStatus.APPROVED);

        List<TreeNodeResponse> children = topics.stream()
            .map(t -> buildTopicNode(t, isAdmin))
            .collect(Collectors.toList());

        return new TreeNodeResponse(
            sub.getId(), sub.getName(), "subject", sub.getStatus(),
            sub.getRejectionReason(), sub.getCreatedBy().getFullName(),
            sub.getCreatedBy().getId(), children);
    }

    private TreeNodeResponse buildTopicNode(Topic topic, boolean isAdmin) {
        var questions = isAdmin
            ? questionRepo.findByTopic(topic)
            : questionRepo.findByTopicAndStatus(topic, ContentStatus.APPROVED);

        List<TreeNodeResponse> children = questions.stream()
            .map(q -> new TreeNodeResponse(
                q.getId(), q.getQuestionText(), "question", q.getStatus(),
                q.getRejectionReason(), q.getCreatedBy().getFullName(),
                q.getCreatedBy().getId(), new ArrayList<>()))
            .collect(Collectors.toList());

        return new TreeNodeResponse(
            topic.getId(), topic.getName(), "topic", topic.getStatus(),
            topic.getRejectionReason(), topic.getCreatedBy().getFullName(),
            topic.getCreatedBy().getId(), children);
    }

    private Category findById(String id) {
        return categoryRepo.findById(java.util.UUID.fromString(id))
            .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
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

    public CategoryResponse toResponse(Category cat) {
        return new CategoryResponse(
            cat.getId(), cat.getName(), cat.getDescription(),
            cat.getStatus(), cat.getRejectionReason(),
            cat.getCreatedBy().getFullName(), cat.getCreatedBy().getId(),
            cat.getCreatedAt(), cat.getUpdatedAt());
    }
}
