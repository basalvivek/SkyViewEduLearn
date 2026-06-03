package com.edulearn.service;

import com.edulearn.dto.request.TeacherRequest;
import com.edulearn.dto.response.TeacherResponse;
import com.edulearn.entity.User;
import com.edulearn.enums.UserRole;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class TeacherService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public TeacherService(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<TeacherResponse> listTeachers() {
        return userRepo.findByRole(UserRole.TEACHER).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public TeacherResponse createTeacher(TeacherRequest request) {
        String password = (request.temporaryPassword() != null && !request.temporaryPassword().isBlank())
                ? request.temporaryPassword()
                : generateTempPassword();
        User teacher = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(password))
                .role(UserRole.TEACHER)
                .jobTitle(request.jobTitle())
                .department(request.department())
                .mustChangePassword(false)
                .isActive(true)
                .build();
        return toResponse(userRepo.save(teacher));
    }

    public TeacherResponse updateTeacher(UUID id, TeacherRequest request) {
        User teacher = findById(id);
        teacher.setFullName(request.fullName());
        teacher.setEmail(request.email());
        teacher.setJobTitle(request.jobTitle());
        teacher.setDepartment(request.department());
        if (request.temporaryPassword() != null && !request.temporaryPassword().isBlank()) {
            teacher.setPasswordHash(passwordEncoder.encode(request.temporaryPassword()));
        }
        return toResponse(userRepo.save(teacher));
    }

    public String resetPassword(UUID id) {
        User teacher = findById(id);
        String newPassword = generateTempPassword();
        teacher.setPasswordHash(passwordEncoder.encode(newPassword));
        teacher.setMustChangePassword(true);
        userRepo.save(teacher);
        return newPassword;
    }

    public void deactivateTeacher(UUID id) {
        User teacher = findById(id);
        teacher.setActive(false);
        userRepo.save(teacher);
    }

    public void deleteTeacher(UUID id) {
        userRepo.delete(findById(id));
    }

    private User findById(UUID id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + id));
    }

    private TeacherResponse toResponse(User teacher) {
        return new TeacherResponse(
                teacher.getId(),
                teacher.getFullName(),
                teacher.getEmail(),
                teacher.getJobTitle(),
                teacher.getDepartment(),
                teacher.isActive(),
                teacher.getCreatedAt()
        );
    }

    private String generateTempPassword() {
        return "Teach" + UUID.randomUUID().toString().substring(0, 8) + "!";
    }
}
