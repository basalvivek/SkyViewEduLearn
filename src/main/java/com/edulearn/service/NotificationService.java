package com.edulearn.service;

import com.edulearn.dto.response.NotificationResponse;
import com.edulearn.entity.Notification;
import com.edulearn.entity.User;
import com.edulearn.enums.UserRole;
import com.edulearn.repository.NotificationRepository;
import com.edulearn.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository repo;
    private final UserRepository userRepo;

    public NotificationService(NotificationRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    public Page<NotificationResponse> getForUser(String email, Pageable pageable) {
        User user = findByEmail(email);
        return repo.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(NotificationResponse::from);
    }

    public long getUnreadCount(String email) {
        User user = findByEmail(email);
        return repo.countByUserIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public void markRead(UUID notifId, String email) {
        User user = findByEmail(email);
        repo.findById(notifId).ifPresent(n -> {
            if (n.getUser().getId().equals(user.getId())) {
                n.setRead(true);
                repo.save(n);
            }
        });
    }

    @Transactional
    public void markAllRead(String email) {
        User user = findByEmail(email);
        repo.markAllReadByUserId(user.getId());
    }

    public void createForUser(UUID userId, String message, String link) {
        userRepo.findById(userId).ifPresent(user -> {
            Notification n = new Notification();
            n.setUser(user);
            n.setMessage(message);
            n.setLink(link);
            repo.save(n);
        });
    }

    public void createForAllAdmins(String message, String link) {
        userRepo.findByRole(UserRole.ADMIN).stream()
                .filter(User::isActive)
                .forEach(admin -> createForUser(admin.getId(), message, link));
    }

    private User findByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new com.edulearn.exception.ResourceNotFoundException("User not found"));
    }
}
