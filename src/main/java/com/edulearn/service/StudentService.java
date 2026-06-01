package com.edulearn.service;

import com.edulearn.dto.request.StudentCreateRequest;
import com.edulearn.dto.response.StudentResponse;
import com.edulearn.entity.Classroom;
import com.edulearn.entity.StudentClass;
import com.edulearn.entity.User;
import com.edulearn.enums.UserRole;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.repository.ClassroomRepository;
import com.edulearn.repository.StudentClassRepository;
import com.edulearn.repository.UserRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentService {

    private final UserRepository userRepo;
    private final ClassroomRepository classroomRepo;
    private final StudentClassRepository studentClassRepo;
    private final PasswordEncoder passwordEncoder;

    public StudentService(UserRepository userRepo,
                          ClassroomRepository classroomRepo,
                          StudentClassRepository studentClassRepo,
                          PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.classroomRepo = classroomRepo;
        this.studentClassRepo = studentClassRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> listStudents() {
        return userRepo.findAll().stream()
                .filter(u -> u.getRole() == UserRole.STUDENT)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public StudentResponse createStudent(StudentCreateRequest request) {
        String password = (request.temporaryPassword() != null && !request.temporaryPassword().isBlank())
                ? request.temporaryPassword()
                : generateTempPassword();

        User student = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(password))
                .role(UserRole.STUDENT)
                .isActive(true)
                .build();
        student = userRepo.save(student);

        if (request.classId() != null) {
            Classroom classroom = classroomRepo.findById(request.classId())
                    .orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + request.classId()));
            StudentClass sc = StudentClass.builder()
                    .student(student)
                    .classroom(classroom)
                    .joinedAt(OffsetDateTime.now())
                    .isActive(true)
                    .build();
            studentClassRepo.save(sc);
        }
        return toResponse(student);
    }

    public StudentResponse updateStudent(UUID id, StudentCreateRequest request) {
        User student = findStudentById(id);
        student.setFullName(request.fullName());
        student.setEmail(request.email());
        if (request.temporaryPassword() != null && !request.temporaryPassword().isBlank()) {
            student.setPasswordHash(passwordEncoder.encode(request.temporaryPassword()));
        }
        return toResponse(userRepo.save(student));
    }

    public void deactivateStudent(UUID id) {
        User student = findStudentById(id);
        student.setActive(false);
        userRepo.save(student);
    }

    public String resetPassword(UUID id) {
        User student = findStudentById(id);
        String newPassword = generateTempPassword();
        student.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(student);
        return newPassword;
    }

    public void assignToClass(UUID studentId, UUID classId) {
        User student = findStudentById(studentId);
        Classroom classroom = classroomRepo.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + classId));

        java.util.Optional<StudentClass> existing = studentClassRepo.findByStudentAndClassroom(student, classroom);
        if (existing.isPresent()) {
            StudentClass sc = existing.get();
            if (sc.isActive()) throw new IllegalStateException("Student already in this class");
            sc.setActive(true);
            studentClassRepo.save(sc);
            return;
        }

        StudentClass sc = StudentClass.builder()
                .student(student)
                .classroom(classroom)
                .joinedAt(OffsetDateTime.now())
                .isActive(true)
                .build();
        studentClassRepo.save(sc);
    }

    public void removeFromClass(UUID studentId, UUID classId) {
        User student = findStudentById(studentId);
        Classroom classroom = classroomRepo.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + classId));
        StudentClass sc = studentClassRepo.findByStudentAndClassroom(student, classroom)
                .orElseThrow(() -> new ResourceNotFoundException("Student is not in this class"));
        sc.setActive(false);
        studentClassRepo.save(sc);
    }

    public Map<String, Object> importFromCsv(MultipartFile file) {
        int created = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] header = reader.readNext(); // skip header row
            String[] line;
            int rowNum = 1;

            while ((line = reader.readNext()) != null) {
                rowNum++;
                try {
                    if (line.length < 2) {
                        errors.add("Row " + rowNum + ": not enough columns");
                        skipped++;
                        continue;
                    }
                    String fullName = line[0].trim();
                    String email = line[1].trim();
                    String yearGroup = line.length > 2 ? line[2].trim() : null;
                    String className = line.length > 3 ? line[3].trim() : null;

                    if (fullName.isEmpty() || email.isEmpty()) {
                        errors.add("Row " + rowNum + ": full_name and email are required");
                        skipped++;
                        continue;
                    }

                    if (userRepo.findByEmail(email).isPresent()) {
                        errors.add("Row " + rowNum + ": email already exists: " + email);
                        skipped++;
                        continue;
                    }

                    UUID classId = null;
                    if (className != null && !className.isEmpty()) {
                        classId = classroomRepo.findByName(className)
                                .map(Classroom::getId)
                                .orElse(null);
                        if (classId == null) {
                            errors.add("Row " + rowNum + ": class not found: " + className + " (student created without class)");
                        }
                    }

                    createStudent(new StudentCreateRequest(fullName, email, classId, yearGroup, null));
                    created++;
                } catch (Exception e) {
                    errors.add("Row " + rowNum + ": " + e.getMessage());
                    skipped++;
                }
            }
        } catch (IOException | CsvValidationException e) {
            errors.add("File parsing error: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("errors", errors);
        return result;
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudent(UUID id) {
        return toResponse(findStudentById(id));
    }

    @Transactional(readOnly = true)
    public List<String> getStudentClasses(UUID id) {
        User student = findStudentById(id);
        return studentClassRepo.findByStudent(student).stream()
                .filter(StudentClass::isActive)
                .map(sc -> sc.getClassroom().getName())
                .collect(Collectors.toList());
    }

    private StudentResponse toResponse(User student) {
        List<String> classes = studentClassRepo.findByStudent(student).stream()
                .filter(StudentClass::isActive)
                .map(sc -> sc.getClassroom().getName())
                .collect(Collectors.toList());
        return new StudentResponse(
                student.getId(),
                student.getFullName(),
                student.getEmail(),
                student.getDisplayName(),
                student.isActive(),
                student.getCreatedAt(),
                classes
        );
    }

    private User findStudentById(UUID id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));
    }

    private String generateTempPassword() {
        return "Temp" + UUID.randomUUID().toString().substring(0, 8) + "!";
    }
}
