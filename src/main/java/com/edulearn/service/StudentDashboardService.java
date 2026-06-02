package com.edulearn.service;

import com.edulearn.dto.response.StudentDashboardResponse;
import com.edulearn.dto.response.StudentExamCardResponse;
import com.edulearn.entity.*;
import com.edulearn.enums.AttemptStatus;
import com.edulearn.enums.ScheduleStatus;
import com.edulearn.exception.ForbiddenException;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StudentDashboardService {

    private final UserRepository userRepo;
    private final StudentClassRepository studentClassRepo;
    private final ExamScheduleRepository scheduleRepo;
    private final ExamAttemptRepository attemptRepo;
    private final ExamQuestionRepository examQuestionRepo;

    public StudentDashboardService(UserRepository userRepo,
                                   StudentClassRepository studentClassRepo,
                                   ExamScheduleRepository scheduleRepo,
                                   ExamAttemptRepository attemptRepo,
                                   ExamQuestionRepository examQuestionRepo) {
        this.userRepo = userRepo;
        this.studentClassRepo = studentClassRepo;
        this.scheduleRepo = scheduleRepo;
        this.attemptRepo = attemptRepo;
        this.examQuestionRepo = examQuestionRepo;
    }

    public StudentDashboardResponse getDashboard(String email) {
        User student = getStudent(email);
        guardPasswordChange(student);

        List<Classroom> classes = getStudentClassrooms(student);
        OffsetDateTime now = OffsetDateTime.now();

        List<ExamSchedule> allSchedules = loadSchedulesForClasses(classes);
        Map<UUID, ExamAttempt> latestAttemptBySchedule = loadLatestAttemptsBySchedule(student);

        long availableExams = allSchedules.stream()
                .filter(s -> s.getStatus() == ScheduleStatus.ACTIVE
                        && !latestAttemptBySchedule.containsKey(s.getId()))
                .count();

        List<ExamAttempt> allAttempts = attemptRepo.findByStudent(student);
        List<ExamAttempt> submitted = allAttempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED)
                .collect(Collectors.toList());
        long examsTaken = submitted.size();

        List<ExamAttempt> marked = submitted.stream()
                .filter(a -> a.getPercentage() != null)
                .collect(Collectors.toList());
        Double averagePercentage = marked.isEmpty() ? null :
                marked.stream().mapToDouble(a -> a.getPercentage().doubleValue()).average().orElse(0);
        Double passRate = marked.isEmpty() ? null :
                marked.stream().filter(a -> Boolean.TRUE.equals(a.getIsPassed())).count() * 100.0 / marked.size();

        List<StudentExamCardResponse> upcoming = allSchedules.stream()
                .filter(s -> s.getStatus() == ScheduleStatus.UPCOMING || s.getStatus() == ScheduleStatus.ACTIVE)
                .filter(s -> {
                    ExamAttempt a = latestAttemptBySchedule.get(s.getId());
                    return a == null || a.getStatus() == AttemptStatus.IN_PROGRESS;
                })
                .sorted(Comparator.comparing(ExamSchedule::getStartAt))
                .limit(5)
                .map(s -> toCard(s, latestAttemptBySchedule.get(s.getId())))
                .collect(Collectors.toList());

        List<StudentExamCardResponse> recent = allAttempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED)
                .sorted(Comparator.comparing(ExamAttempt::getSubmittedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(a -> toCard(a.getSchedule(), a))
                .collect(Collectors.toList());

        return new StudentDashboardResponse(availableExams, examsTaken, averagePercentage, passRate, upcoming, recent);
    }

    public List<StudentExamCardResponse> getActiveExams(String email) {
        User student = getStudent(email);
        guardPasswordChange(student);
        List<Classroom> classes = getStudentClassrooms(student);
        Map<UUID, ExamAttempt> latest = loadLatestAttemptsBySchedule(student);

        List<StudentExamCardResponse> result = new ArrayList<>();
        // In-progress attempts first
        latest.values().stream()
                .filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS)
                .map(a -> toCard(a.getSchedule(), a))
                .forEach(result::add);
        // Active schedules with no attempt yet
        loadSchedulesForClasses(classes).stream()
                .filter(s -> s.getStatus() == ScheduleStatus.ACTIVE && !latest.containsKey(s.getId()))
                .map(s -> toCard(s, null))
                .forEach(result::add);
        return result;
    }

    public List<StudentExamCardResponse> getUpcomingExams(String email) {
        User student = getStudent(email);
        guardPasswordChange(student);
        List<Classroom> classes = getStudentClassrooms(student);
        Set<UUID> attempted = attemptRepo.findByStudent(student).stream()
                .map(a -> a.getSchedule().getId()).collect(Collectors.toSet());
        return loadSchedulesForClasses(classes).stream()
                .filter(s -> s.getStatus() == ScheduleStatus.UPCOMING && !attempted.contains(s.getId()))
                .sorted(Comparator.comparing(ExamSchedule::getStartAt))
                .map(s -> toCard(s, null))
                .collect(Collectors.toList());
    }

    public List<StudentExamCardResponse> getCompletedExams(String email) {
        User student = getStudent(email);
        guardPasswordChange(student);
        return attemptRepo.findByStudent(student).stream()
                .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED)
                .sorted(Comparator.comparing(ExamAttempt::getSubmittedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(a -> toCard(a.getSchedule(), a))
                .collect(Collectors.toList());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void guardPasswordChange(User student) {
        if (student.isMustChangePassword()) {
            throw new ForbiddenException("Password change required before accessing the portal.");
        }
    }

    private List<ExamSchedule> loadSchedulesForClasses(List<Classroom> classes) {
        return classes.stream()
                .flatMap(c -> scheduleRepo.findByClassroomOrderByStartAtAsc(c).stream())
                .filter(s -> !s.isDeleted())
                // Deduplicate by ID (a schedule could appear in multiple classes)
                .collect(Collectors.toMap(ExamSchedule::getId, s -> s, (a, b) -> a))
                .values().stream()
                .collect(Collectors.toList());
    }

    private Map<UUID, ExamAttempt> loadLatestAttemptsBySchedule(User student) {
        return attemptRepo.findByStudent(student).stream()
                .collect(Collectors.toMap(
                        a -> a.getSchedule().getId(),
                        a -> a,
                        // Keep the most recent attempt per schedule
                        (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b
                ));
    }

    private StudentExamCardResponse toCard(ExamSchedule schedule, ExamAttempt attempt) {
        Exam exam = schedule.getExam();
        // COUNT query — no N+1
        int qCount = (int) examQuestionRepo.countByExam(exam);
        return new StudentExamCardResponse(
                schedule.getId(),
                exam.getId(),
                exam.getName(),
                exam.getDescription(),
                exam.getTimeLimitMins(),
                qCount,
                exam.getPassMark(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.getStatus().name(),
                attempt != null ? attempt.getId() : null,
                attempt != null ? attempt.getStatus().name() : null,
                0, // answered count — not loaded here for performance
                attempt != null && attempt.getPercentage() != null ? attempt.getPercentage().doubleValue() : null,
                attempt != null ? attempt.getIsPassed() : null,
                attempt != null ? attempt.getSubmittedAt() : null
        );
    }

    private User getStudent(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private List<Classroom> getStudentClassrooms(User student) {
        return studentClassRepo.findByStudentAndIsActiveTrue(student).stream()
                .map(StudentClass::getClassroom)
                .collect(Collectors.toList());
    }
}
