package com.edulearn.service;

import com.edulearn.dto.request.PasswordChangeRequest;
import com.edulearn.dto.request.ProfileUpdateRequest;
import com.edulearn.dto.response.ProfileResponse;
import com.edulearn.entity.User;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.exception.ValidationException;
import com.edulearn.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ProfileService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload.dir.avatars:uploads/avatars}")
    private String avatarUploadDir;

    public ProfileService(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public ProfileResponse getProfile(String email) {
        return ProfileResponse.from(findByEmail(email));
    }

    @Transactional
    public ProfileResponse updateProfile(String email, ProfileUpdateRequest req) {
        User user = findByEmail(email);
        user.setFullName(req.getFullName());
        user.setDisplayName(req.getDisplayName());
        user.setJobTitle(req.getJobTitle());
        user.setDepartment(req.getDepartment());
        user.setBio(req.getBio());
        user.setPhone(req.getPhone());
        return ProfileResponse.from(userRepo.save(user));
    }

    @Transactional
    public String uploadAvatar(String email, MultipartFile file) throws IOException {
        if (file.getSize() > 1024 * 1024)
            throw new ValidationException("Avatar must be under 1MB");
        String ct = file.getContentType();
        if (ct == null || (!ct.equals("image/png") && !ct.equals("image/jpeg")))
            throw new ValidationException("Only PNG or JPG avatars are allowed");

        Path dir = Paths.get(avatarUploadDir);
        Files.createDirectories(dir);
        String ext = ct.equals("image/png") ? ".png" : ".jpg";
        String filename = "avatar_" + System.currentTimeMillis() + ext;
        Files.copy(file.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

        User user = findByEmail(email);
        user.setAvatarPath("uploads/avatars/" + filename);
        userRepo.save(user);
        return "/uploads/avatars/" + filename;
    }

    @Transactional
    public void changePassword(String email, PasswordChangeRequest req) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash()))
            throw new ValidationException("Current password is incorrect");
        if (!req.getNewPassword().equals(req.getConfirmPassword()))
            throw new ValidationException("Passwords do not match");
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setMustChangePassword(false);
        userRepo.save(user);
    }

    public ProfileResponse getProfileById(UUID id) {
        return ProfileResponse.from(userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    private User findByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
