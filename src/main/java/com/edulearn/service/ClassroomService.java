package com.edulearn.service;

import com.edulearn.dto.request.ClassroomRequest;
import com.edulearn.dto.response.ClassroomResponse;
import com.edulearn.entity.Classroom;
import com.edulearn.entity.User;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.repository.ClassroomRepository;
import com.edulearn.repository.StudentClassRepository;
import com.edulearn.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClassroomService {

    private final ClassroomRepository classroomRepo;
    private final UserRepository userRepo;
    private final StudentClassRepository studentClassRepo;

    public ClassroomService(ClassroomRepository classroomRepo,
                            UserRepository userRepo,
                            StudentClassRepository studentClassRepo) {
        this.classroomRepo = classroomRepo;
        this.userRepo = userRepo;
        this.studentClassRepo = studentClassRepo;
    }

    @Transactional(readOnly = true)
    public List<ClassroomResponse> listClassrooms() {
        return classroomRepo.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ClassroomResponse createClassroom(ClassroomRequest request, String email) {
        User actor = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        Classroom classroom = Classroom.builder()
                .name(request.name())
                .yearGroup(request.yearGroup())
                .createdBy(actor)
                .build();
        return toResponse(classroomRepo.save(classroom));
    }

    public ClassroomResponse updateClassroom(UUID id, ClassroomRequest request) {
        Classroom classroom = findById(id);
        classroom.setName(request.name());
        classroom.setYearGroup(request.yearGroup());
        return toResponse(classroomRepo.save(classroom));
    }

    public void deleteClassroom(UUID id) {
        Classroom classroom = findById(id);
        classroom.setActive(false);
        classroomRepo.save(classroom);
    }

    public ClassroomResponse toResponse(Classroom classroom) {
        int studentCount = studentClassRepo.findByClassroomAndIsActiveTrue(classroom).size();
        return new ClassroomResponse(
                classroom.getId(),
                classroom.getName(),
                classroom.getYearGroup(),
                classroom.isActive(),
                studentCount
        );
    }

    public Classroom findById(UUID id) {
        return classroomRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + id));
    }
}
