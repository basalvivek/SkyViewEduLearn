package com.edulearn.service;

import com.edulearn.dto.request.ApprovalActionRequest;
import com.edulearn.dto.request.SubjectRequest;
import com.edulearn.dto.response.SubjectResponse;
import com.edulearn.entity.Category;
import com.edulearn.entity.Subject;
import com.edulearn.entity.User;
import com.edulearn.enums.ContentStatus;
import com.edulearn.enums.UserRole;
import com.edulearn.exception.ForbiddenException;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.repository.CategoryRepository;
import com.edulearn.repository.SubjectRepository;
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
public class SubjectService {

    private final SubjectRepository subjectRepo;
    private final CategoryRepository categoryRepo;
    private final UserRepository userRepo;
    private final ContentApprovalService approvalService;

    public SubjectService(SubjectRepository subjectRepo, CategoryRepository categoryRepo,
                          UserRepository userRepo, ContentApprovalService approvalService) {
        this.subjectRepo = subjectRepo;
        this.categoryRepo = categoryRepo;
        this.userRepo = userRepo;
        this.approvalService = approvalService;
    }

    public SubjectResponse create(String categoryId, SubjectRequest request, String actorEmail) {
        User actor = getUser(actorEmail);
        Category category = findCategory(categoryId);
        Subject sub = Subject.builder()
            .category(category)
            .name(request.name())
            .curriculumLevel(request.curriculumLevel())
            .description(request.description())
            .createdBy(actor)
            .status(actor.getRole() == UserRole.ADMIN ? ContentStatus.APPROVED : ContentStatus.DRAFT)
            .build();
        if (actor.getRole() == UserRole.ADMIN) sub.setApprovedBy(actor);
        return toResponse(subjectRepo.save(sub));
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> listByCategory(String categoryId, String actorEmail) {
        Category category = findCategory(categoryId);
        boolean isAdmin = actorEmail != null && getUser(actorEmail).getRole() == UserRole.ADMIN;
        List<Subject> subjects = isAdmin
            ? subjectRepo.findByCategory(category)
            : subjectRepo.findByCategoryAndStatus(category, ContentStatus.APPROVED);
        return subjects.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubjectResponse get(String id) {
        return toResponse(findById(id));
    }

    public SubjectResponse update(String id, SubjectRequest request, String actorEmail) {
        User actor = getUser(actorEmail);
        Subject sub = findById(id);
        checkOwnerOrAdmin(sub.getCreatedBy(), actor);
        sub.setName(request.name());
        sub.setCurriculumLevel(request.curriculumLevel());
        sub.setDescription(request.description());
        sub.setUpdatedBy(actor);
        if (actor.getRole() != UserRole.ADMIN && sub.getStatus() == ContentStatus.APPROVED) {
            sub.setStatus(ContentStatus.DRAFT);
        }
        return toResponse(subjectRepo.save(sub));
    }

    public void delete(String id, String actorEmail) {
        User actor = getUser(actorEmail);
        if (actor.getRole() != UserRole.ADMIN) throw new ForbiddenException("Admin only");
        subjectRepo.delete(findById(id));
    }

    public SubjectResponse submit(String id, String actorEmail) {
        User actor = getUser(actorEmail);
        Subject sub = findById(id);
        checkOwnerOrAdmin(sub.getCreatedBy(), actor);
        approvalService.submit(sub, actor);
        return toResponse(subjectRepo.save(sub));
    }

    public SubjectResponse approve(String id, ApprovalActionRequest req, String actorEmail) {
        User admin = getUser(actorEmail);
        if (admin.getRole() != UserRole.ADMIN) throw new ForbiddenException("Admin only");
        Subject sub = findById(id);
        approvalService.approve(sub, admin, req != null ? req.note() : null);
        return toResponse(subjectRepo.save(sub));
    }

    public SubjectResponse reject(String id, ApprovalActionRequest req, String actorEmail) {
        User admin = getUser(actorEmail);
        if (admin.getRole() != UserRole.ADMIN) throw new ForbiddenException("Admin only");
        Subject sub = findById(id);
        approvalService.reject(sub, admin, req != null ? req.note() : null);
        return toResponse(subjectRepo.save(sub));
    }

    @Transactional(readOnly = true)
    public Page<SubjectResponse> findByStatus(ContentStatus status, Pageable pageable) {
        return subjectRepo.findByStatus(status, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SubjectResponse> findByStatusAndCreatedBy(ContentStatus status, User user, Pageable pageable) {
        return subjectRepo.findByStatusAndCreatedBy(status, user, pageable).map(this::toResponse);
    }

    private Subject findById(String id) {
        return subjectRepo.findById(UUID.fromString(id))
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + id));
    }

    private Category findCategory(String id) {
        return categoryRepo.findById(UUID.fromString(id))
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

    public SubjectResponse toResponse(Subject sub) {
        return new SubjectResponse(
            sub.getId(), sub.getCategory().getId(),
            sub.getName(), sub.getCurriculumLevel(), sub.getDescription(),
            sub.getStatus(), sub.getRejectionReason(),
            sub.getCreatedBy().getFullName(), sub.getCreatedBy().getId(),
            sub.getCreatedAt(), sub.getUpdatedAt());
    }
}
