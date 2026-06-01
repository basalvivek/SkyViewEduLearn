package com.edulearn.service;

import com.edulearn.dto.request.ScheduleRequest;
import com.edulearn.dto.response.ScheduleResponse;
import com.edulearn.entity.Classroom;
import com.edulearn.entity.Exam;
import com.edulearn.entity.ExamSchedule;
import com.edulearn.entity.User;
import com.edulearn.enums.ScheduleStatus;
import com.edulearn.enums.UserRole;
import com.edulearn.exception.ForbiddenException;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.repository.ClassroomRepository;
import com.edulearn.repository.ExamAttemptRepository;
import com.edulearn.repository.ExamRepository;
import com.edulearn.repository.ExamScheduleRepository;
import com.edulearn.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ScheduleService {

    private final ExamScheduleRepository scheduleRepo;
    private final ClassroomRepository classroomRepo;
    private final ExamRepository examRepo;
    private final UserRepository userRepo;
    private final ExamAttemptRepository attemptRepo;

    public ScheduleService(ExamScheduleRepository scheduleRepo,
                           ClassroomRepository classroomRepo,
                           ExamRepository examRepo,
                           UserRepository userRepo,
                           ExamAttemptRepository attemptRepo) {
        this.scheduleRepo = scheduleRepo;
        this.classroomRepo = classroomRepo;
        this.examRepo = examRepo;
        this.userRepo = userRepo;
        this.attemptRepo = attemptRepo;
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> listSchedules(String email) {
        User actor = getUser(email);
        List<ExamSchedule> schedules;
        if (actor.getRole() == UserRole.ADMIN) {
            schedules = scheduleRepo.findAll();
        } else {
            schedules = scheduleRepo.findAll(); // teachers can see all schedules they're involved with
        }
        return schedules.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ScheduleResponse createSchedule(ScheduleRequest request, String email) {
        User actor = getUser(email);
        Exam exam = examRepo.findById(request.examId())
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + request.examId()));
        Classroom classroom = classroomRepo.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + request.classId()));

        ExamSchedule schedule = ExamSchedule.builder()
                .exam(exam)
                .classroom(classroom)
                .startAt(request.startAt())
                .endAt(request.endAt())
                .maxAttempts((short) request.maxAttempts())
                .showResultsImmediately(request.showResultsImmediately())
                .instructions(request.instructions())
                .status(ScheduleStatus.UPCOMING)
                .createdBy(actor)
                .build();
        return toResponse(scheduleRepo.save(schedule));
    }

    @Transactional(readOnly = true)
    public ScheduleResponse getSchedule(UUID id) {
        return toResponse(findById(id));
    }

    public ScheduleResponse updateSchedule(UUID id, ScheduleRequest request, String email) {
        User actor = getUser(email);
        ExamSchedule schedule = findById(id);
        Exam exam = examRepo.findById(request.examId())
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + request.examId()));
        Classroom classroom = classroomRepo.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + request.classId()));

        schedule.setExam(exam);
        schedule.setClassroom(classroom);
        schedule.setStartAt(request.startAt());
        schedule.setEndAt(request.endAt());
        schedule.setMaxAttempts((short) request.maxAttempts());
        schedule.setShowResultsImmediately(request.showResultsImmediately());
        schedule.setInstructions(request.instructions());
        schedule.setUpdatedBy(actor);
        return toResponse(scheduleRepo.save(schedule));
    }

    public void cancelSchedule(UUID id, String email) {
        User actor = getUser(email);
        if (actor.getRole() != UserRole.ADMIN) throw new ForbiddenException("Admin only");
        ExamSchedule schedule = findById(id);
        schedule.setStatus(ScheduleStatus.CANCELLED);
        scheduleRepo.save(schedule);
    }

    public ScheduleResponse toResponse(ExamSchedule schedule) {
        int attemptCount = attemptRepo.findBySchedule(schedule).size();
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getExam().getId(),
                schedule.getExam().getName(),
                schedule.getClassroom().getId(),
                schedule.getClassroom().getName(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.getMaxAttempts(),
                schedule.isShowResultsImmediately(),
                schedule.getInstructions(),
                schedule.getStatus(),
                attemptCount,
                schedule.getCreatedBy() != null ? schedule.getCreatedBy().getFullName() : null
        );
    }

    public ExamSchedule findById(UUID id) {
        return scheduleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + id));
    }

    private User getUser(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
