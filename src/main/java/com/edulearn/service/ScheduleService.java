package com.edulearn.service;

import com.edulearn.dto.request.ClassEventRequest;
import com.edulearn.dto.request.MoveScheduleRequest;
import com.edulearn.dto.request.RangeScheduleRequest;
import com.edulearn.dto.request.RecurringScheduleRequest;
import com.edulearn.dto.request.ScheduleRequest;
import com.edulearn.dto.response.CalendarEntryResponse;
import com.edulearn.dto.response.ScheduleResponse;
import com.edulearn.dto.response.SeriesCreatedResponse;
import com.edulearn.entity.Classroom;
import com.edulearn.entity.Exam;
import com.edulearn.entity.ExamSchedule;
import com.edulearn.entity.User;
import com.edulearn.enums.RecurrenceType;
import com.edulearn.enums.ScheduleStatus;
import com.edulearn.enums.ScheduleType;
import com.edulearn.enums.SeriesMoveScope;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
        User assignedTo = resolveAssignedTo(request.teacherId(), actor);

        ExamSchedule schedule = ExamSchedule.builder()
                .scheduleType(ScheduleType.EXAM)
                .exam(exam)
                .classroom(classroom)
                .startAt(request.startAt())
                .endAt(request.endAt())
                .maxAttempts((short) request.maxAttempts())
                .showResultsImmediately(request.showResultsImmediately())
                .instructions(request.instructions())
                .status(ScheduleStatus.UPCOMING)
                .assignedTo(assignedTo)
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

        schedule.setScheduleType(ScheduleType.EXAM);
        schedule.setExam(exam);
        schedule.setClassroom(classroom);
        schedule.setStartAt(request.startAt());
        schedule.setEndAt(request.endAt());
        schedule.setMaxAttempts((short) request.maxAttempts());
        schedule.setShowResultsImmediately(request.showResultsImmediately());
        schedule.setInstructions(request.instructions());
        schedule.setAssignedTo(resolveAssignedTo(request.teacherId(), actor));
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
                schedule.getExam() != null ? schedule.getExam().getId() : null,
                schedule.getExam() != null ? schedule.getExam().getName() : null,
                schedule.getClassroom() != null ? schedule.getClassroom().getId() : null,
                schedule.getClassroom() != null ? schedule.getClassroom().getName() : null,
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

    public ScheduleResponse updateClassEvent(UUID id, ClassEventRequest req, String email) {
        User actor = getUser(email);
        ExamSchedule schedule = findById(id);
        Classroom classroom = req.classId() != null
                ? classroomRepo.findById(req.classId())
                        .orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + req.classId()))
                : null;
        schedule.setScheduleType(req.scheduleType());
        schedule.setTitle(req.title());
        schedule.setClassroom(classroom);
        schedule.setStartAt(req.startAt());
        schedule.setEndAt(req.endAt());
        schedule.setAssignedTo(resolveAssignedTo(req.teacherId(), actor));
        schedule.setUpdatedBy(actor);
        return toResponse(scheduleRepo.save(schedule));
    }

    // ── Calendar feed ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CalendarEntryResponse> getCalendar(int year, int month, UUID classId, String email) {
        User actor = getUser(email);
        OffsetDateTime from = OffsetDateTime.of(year, month, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime to = from.plusMonths(1).minusNanos(1);
        List<ExamSchedule> list;
        if (actor.getRole() == UserRole.TEACHER) {
            list = classId != null
                    ? scheduleRepo.findByAssignedToAndClassroomIdAndStartAtBetweenOrderByStartAtAsc(actor, classId, from, to)
                    : scheduleRepo.findByAssignedToAndStartAtBetweenOrderByStartAtAsc(actor, from, to);
        } else {
            list = classId != null
                    ? scheduleRepo.findByClassroomIdAndStartAtBetweenOrderByStartAtAsc(classId, from, to)
                    : scheduleRepo.findByStartAtBetweenOrderByStartAtAsc(from, to);
        }
        return list.stream().map(this::toCalendarEntry).collect(Collectors.toList());
    }

    public CalendarEntryResponse toCalendarEntry(ExamSchedule s) {
        return new CalendarEntryResponse(
                s.getId(),
                s.getScheduleType() != null ? s.getScheduleType().name() : "EXAM",
                s.getTitle() != null ? s.getTitle() : (s.getExam() != null ? s.getExam().getName() : ""),
                s.getClassroom() != null ? s.getClassroom().getId() : null,
                s.getClassroom() != null ? s.getClassroom().getName() : null,
                s.getStartAt(),
                s.getEndAt(),
                s.getStatus().name(),
                s.getSeriesId(),
                s.getRecurrenceType() != null ? s.getRecurrenceType().name() : "NONE",
                s.getCreatedBy() != null ? s.getCreatedBy().getFullName() : null,
                s.getExam() != null ? s.getExam().getId() : null,
                s.getExam() != null ? s.getExam().getName() : null,
                s.getAssignedTo() != null ? s.getAssignedTo().getId() : null,
                s.getAssignedTo() != null ? s.getAssignedTo().getFullName() : null
        );
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> listTeachers() {
        return userRepo.findByRole(UserRole.TEACHER).stream()
                .map(u -> {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("name", u.getFullName());
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ── Class / Event / Holiday (single, no exam) ──────────────

    public ScheduleResponse createClassEvent(ClassEventRequest req, String email) {
        User actor = getUser(email);
        Classroom classroom = req.classId() != null
                ? classroomRepo.findById(req.classId())
                        .orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + req.classId()))
                : null;
        ExamSchedule s = ExamSchedule.builder()
                .scheduleType(req.scheduleType())
                .title(req.title())
                .classroom(classroom)
                .startAt(req.startAt())
                .endAt(req.endAt())
                .status(ScheduleStatus.UPCOMING)
                .assignedTo(resolveAssignedTo(req.teacherId(), actor))
                .createdBy(actor)
                .build();
        return toResponse(scheduleRepo.save(s));
    }

    // ── Date-range bulk booking ────────────────────────────────

    public SeriesCreatedResponse createRange(RangeScheduleRequest req, String email) {
        User actor = getUser(email);
        Classroom classroom = req.classId() != null
                ? classroomRepo.findById(req.classId())
                        .orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + req.classId()))
                : null;
        UUID seriesId = UUID.randomUUID();
        LocalTime time = parseTime(req.startTime());
        List<ExamSchedule> toSave = new ArrayList<>();
        LocalDate d = req.fromDate();
        while (!d.isAfter(req.toDate())) {
            OffsetDateTime start = OffsetDateTime.of(d, time, ZoneOffset.UTC);
            OffsetDateTime end = start.plusMinutes(req.durationMinutes());
            toSave.add(ExamSchedule.builder()
                    .scheduleType(req.scheduleType())
                    .title(req.title())
                    .classroom(classroom)
                    .startAt(start).endAt(end)
                    .status(ScheduleStatus.UPCOMING)
                    .seriesId(seriesId)
                    .recurrenceType(RecurrenceType.DAILY)
                    .seriesStart(req.fromDate()).seriesEnd(req.toDate())
                    .assignedTo(resolveAssignedTo(req.teacherId(), actor))
                    .createdBy(actor)
                    .build());
            d = d.plusDays(1);
        }
        scheduleRepo.saveAll(toSave);
        return new SeriesCreatedResponse(seriesId, toSave.size());
    }

    // ── Recurring series booking ───────────────────────────────

    public SeriesCreatedResponse createRecurring(RecurringScheduleRequest req, String email) {
        User actor = getUser(email);
        Classroom classroom = req.classId() != null
                ? classroomRepo.findById(req.classId())
                        .orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + req.classId()))
                : null;
        UUID seriesId = UUID.randomUUID();
        LocalTime time = parseTime(req.startTime());
        List<LocalDate> dates = generateDates(req);
        List<ExamSchedule> toSave = new ArrayList<>();
        for (LocalDate d : dates) {
            OffsetDateTime start = OffsetDateTime.of(d, time, ZoneOffset.UTC);
            OffsetDateTime end = start.plusMinutes(req.durationMinutes());
            toSave.add(ExamSchedule.builder()
                    .scheduleType(req.scheduleType())
                    .title(req.title())
                    .classroom(classroom)
                    .startAt(start).endAt(end)
                    .status(ScheduleStatus.UPCOMING)
                    .seriesId(seriesId)
                    .recurrenceType(req.recurrenceType())
                    .recurrenceDays(req.recurrenceDays())
                    .seriesStart(req.seriesStart()).seriesEnd(req.seriesEnd())
                    .assignedTo(resolveAssignedTo(req.teacherId(), actor))
                    .createdBy(actor)
                    .build());
        }
        scheduleRepo.saveAll(toSave);
        return new SeriesCreatedResponse(seriesId, toSave.size());
    }

    // ── Drag-drop move ─────────────────────────────────────────

    public void moveSchedule(UUID id, MoveScheduleRequest req, String email) {
        User actor = getUser(email);
        ExamSchedule ev = findById(id);
        if (actor.getRole() != UserRole.ADMIN && (ev.getCreatedBy() == null || !ev.getCreatedBy().getId().equals(actor.getId()))) {
            throw new ForbiddenException("Cannot move another user's event");
        }
        long offsetDays = ChronoUnit.DAYS.between(ev.getStartAt().toLocalDate(), req.newDate());
        if (offsetDays == 0) return;

        SeriesMoveScope scope = req.applyTo() != null ? req.applyTo() : SeriesMoveScope.THIS;

        if (scope == SeriesMoveScope.THIS || ev.getSeriesId() == null) {
            ev.setSeriesId(null);
            ev.setStartAt(ev.getStartAt().plusDays(offsetDays));
            ev.setEndAt(ev.getEndAt().plusDays(offsetDays));
            scheduleRepo.save(ev);
        } else {
            List<ExamSchedule> affected = scope == SeriesMoveScope.ALL
                    ? scheduleRepo.findBySeriesIdOrderByStartAtAsc(ev.getSeriesId())
                    : scheduleRepo.findBySeriesIdAndStartAtGreaterThanEqualOrderByStartAtAsc(ev.getSeriesId(), ev.getStartAt());
            for (ExamSchedule s : affected) {
                s.setStartAt(s.getStartAt().plusDays(offsetDays));
                s.setEndAt(s.getEndAt().plusDays(offsetDays));
            }
            scheduleRepo.saveAll(affected);
        }
    }

    // ── Series GET / DELETE ────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CalendarEntryResponse> getSeries(UUID seriesId) {
        return scheduleRepo.findBySeriesIdOrderByStartAtAsc(seriesId)
                .stream().map(this::toCalendarEntry).collect(Collectors.toList());
    }

    public void deleteSeries(UUID seriesId, String email) {
        User actor = getUser(email);
        if (actor.getRole() != UserRole.ADMIN) throw new ForbiddenException("Admin only");
        scheduleRepo.findBySeriesIdOrderByStartAtAsc(seriesId)
                .forEach(scheduleRepo::delete);
    }

    // ── Helpers ────────────────────────────────────────────────

    private List<LocalDate> generateDates(RecurringScheduleRequest req) {
        List<LocalDate> dates = new ArrayList<>();
        Set<Integer> targetDows = parseDowSet(req.recurrenceDays());
        LocalDate d = req.seriesStart();

        while (!d.isAfter(req.seriesEnd())) {
            if (req.recurrenceType() == RecurrenceType.MONTHLY) {
                dates.add(d);
                d = d.plusMonths(1);
            } else if (req.recurrenceType() == RecurrenceType.FORTNIGHTLY) {
                int dow = d.getDayOfWeek().getValue(); // Mon=1 … Sun=7
                if (targetDows.isEmpty() || targetDows.contains(dow)) dates.add(d);
                d = d.plusDays(targetDows.isEmpty() || targetDows.contains(dow) ? 14 : 1);
            } else {
                int dow = d.getDayOfWeek().getValue();
                if (targetDows.isEmpty() || targetDows.contains(dow)) dates.add(d);
                d = d.plusDays(1);
            }
        }
        return dates;
    }

    private Set<Integer> parseDowSet(String days) {
        Set<Integer> set = new LinkedHashSet<>();
        if (days == null || days.isBlank()) return set;
        for (String part : days.split(",")) {
            try { set.add(Integer.parseInt(part.trim())); } catch (NumberFormatException ignored) {}
        }
        return set;
    }

    private LocalTime parseTime(String hhmm) {
        if (hhmm == null || !hhmm.contains(":")) return LocalTime.of(9, 0);
        String[] parts = hhmm.split(":");
        return LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    public ExamSchedule findById(UUID id) {
        return scheduleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + id));
    }

    private User getUser(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private User resolveAssignedTo(UUID teacherId, User actor) {
        if (teacherId != null) return userRepo.findById(teacherId).orElse(null);
        if (actor.getRole() == UserRole.TEACHER) return actor;
        return null;
    }
}
