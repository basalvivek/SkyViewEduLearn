package com.edulearn.dto.response;

import com.edulearn.entity.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ProfileResponse {
    private UUID id;
    private String fullName;
    private String displayName;
    private String email;
    private String jobTitle;
    private String department;
    private String bio;
    private String phone;
    private String avatarUrl;
    private String role;
    private boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastLoginAt;

    public static ProfileResponse from(User u) {
        ProfileResponse r = new ProfileResponse();
        r.id = u.getId();
        r.fullName = u.getFullName();
        r.displayName = u.getDisplayName();
        r.email = u.getEmail();
        r.jobTitle = u.getJobTitle();
        r.department = u.getDepartment();
        r.bio = u.getBio();
        r.phone = u.getPhone();
        r.avatarUrl = u.getAvatarPath() != null ? "/" + u.getAvatarPath() : null;
        r.role = u.getRole().name();
        r.isActive = u.isActive();
        r.createdAt = u.getCreatedAt();
        r.lastLoginAt = u.getLastLoginAt();
        return r;
    }

    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getJobTitle() { return jobTitle; }
    public String getDepartment() { return department; }
    public String getBio() { return bio; }
    public String getPhone() { return phone; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getRole() { return role; }
    public boolean isActive() { return isActive; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getLastLoginAt() { return lastLoginAt; }
}
