package com.edulearn.service;

import com.edulearn.entity.AttemptAnswer;
import com.edulearn.entity.ExamAttempt;
import com.edulearn.entity.Question;
import com.edulearn.entity.QuestionOption;
import com.edulearn.enums.QuestionType;
import com.edulearn.repository.AttemptAnswerRepository;
import com.edulearn.repository.ExamAttemptRepository;
import com.edulearn.repository.QuestionOptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AutoMarkingService {

    private final AttemptAnswerRepository answerRepo;
    private final ExamAttemptRepository attemptRepo;
    private final QuestionOptionRepository optionRepo;

    public AutoMarkingService(AttemptAnswerRepository answerRepo,
                              ExamAttemptRepository attemptRepo,
                              QuestionOptionRepository optionRepo) {
        this.answerRepo = answerRepo;
        this.attemptRepo = attemptRepo;
        this.optionRepo = optionRepo;
    }

    public void markAttempt(ExamAttempt attempt) {
        List<AttemptAnswer> answers = answerRepo.findByAttempt(attempt);
        short totalObtained = 0;
        int autoMarked = 0;

        for (AttemptAnswer answer : answers) {
            Question question = answer.getQuestion();
            QuestionType type = question.getQuestionType();

            if (type == QuestionType.MCQ_SINGLE) {
                autoMarked++;
                List<UUID> selected = parseSelectedOptionIds(answer.getSelectedOptionIds());
                if (selected.size() == 1) {
                    QuestionOption option = optionRepo.findById(selected.get(0)).orElse(null);
                    if (option != null && option.isCorrect()) {
                        short marks = question.getMarks() != null ? question.getMarks() : 1;
                        answer.setMarksAwarded(marks);
                        answer.setIsCorrect(true);
                        totalObtained += marks;
                    } else {
                        answer.setMarksAwarded((short) 0);
                        answer.setIsCorrect(false);
                    }
                } else {
                    answer.setMarksAwarded((short) 0);
                    answer.setIsCorrect(false);
                }
                answerRepo.save(answer);

            } else if (type == QuestionType.MCQ_MULTIPLE) {
                autoMarked++;
                List<UUID> selected = parseSelectedOptionIds(answer.getSelectedOptionIds());
                List<QuestionOption> allOptions = optionRepo.findByQuestionOrderByDisplayOrderAsc(question);
                List<UUID> correctOptionIds = allOptions.stream()
                        .filter(QuestionOption::isCorrect)
                        .map(QuestionOption::getId)
                        .collect(Collectors.toList());

                boolean allCorrectSelected = selected.containsAll(correctOptionIds)
                        && correctOptionIds.containsAll(selected);

                if (allCorrectSelected) {
                    short marks = question.getMarks() != null ? question.getMarks() : 1;
                    answer.setMarksAwarded(marks);
                    answer.setIsCorrect(true);
                    totalObtained += marks;
                } else {
                    // Partial marking: award marks for each correct option selected, minus penalty for wrong
                    // Simple approach: count correct selections / total correct * marks
                    long correctSelected = selected.stream().filter(correctOptionIds::contains).count();
                    long wrongSelected = selected.stream().filter(id -> !correctOptionIds.contains(id)).count();
                    short maxMarks = question.getMarks() != null ? question.getMarks() : 1;

                    if (correctSelected > 0 && wrongSelected == 0) {
                        // Some correct, none wrong — partial credit
                        short partial = (short) Math.round((double) correctSelected / correctOptionIds.size() * maxMarks);
                        answer.setMarksAwarded(partial);
                        answer.setIsCorrect(false);
                        totalObtained += partial;
                    } else {
                        answer.setMarksAwarded((short) 0);
                        answer.setIsCorrect(false);
                    }
                }
                answerRepo.save(answer);

            } else if (type == QuestionType.TRUE_FALSE) {
                autoMarked++;
                Boolean studentAnswer = answer.getBooleanAnswer();
                Boolean correctAnswer = question.getCorrectBoolean();
                if (studentAnswer != null && correctAnswer != null && studentAnswer.equals(correctAnswer)) {
                    short marks = question.getMarks() != null ? question.getMarks() : 1;
                    answer.setMarksAwarded(marks);
                    answer.setIsCorrect(true);
                    totalObtained += marks;
                } else {
                    answer.setMarksAwarded((short) 0);
                    answer.setIsCorrect(false);
                }
                answerRepo.save(answer);

            } else if (type == QuestionType.IMAGE_BASED) {
                // IMAGE_BASED with MCQ mode can be auto-marked
                if (question.getImageAnswerType() != null
                        && question.getImageAnswerType().name().equals("MCQ")) {
                    autoMarked++;
                    List<UUID> selected = parseSelectedOptionIds(answer.getSelectedOptionIds());
                    if (selected.size() == 1) {
                        QuestionOption option = optionRepo.findById(selected.get(0)).orElse(null);
                        if (option != null && option.isCorrect()) {
                            short marks = question.getMarks() != null ? question.getMarks() : 1;
                            answer.setMarksAwarded(marks);
                            answer.setIsCorrect(true);
                            totalObtained += marks;
                        } else {
                            answer.setMarksAwarded((short) 0);
                            answer.setIsCorrect(false);
                        }
                    } else {
                        answer.setMarksAwarded((short) 0);
                        answer.setIsCorrect(false);
                    }
                    answerRepo.save(answer);
                }
                // WRITTEN type stays unmarked — manual marking required
            }
            // SHORT_ANSWER, ESSAY, CODE — require manual marking; leave marksAwarded null
        }

        // Update attempt with running totals for auto-marked questions
        attempt.setMarksObtained(totalObtained);
        if (attempt.getTotalMarks() != null && attempt.getTotalMarks() > 0) {
            BigDecimal pct = BigDecimal.valueOf((double) totalObtained / attempt.getTotalMarks() * 100)
                    .setScale(2, RoundingMode.HALF_UP);
            attempt.setPercentage(pct);
            if (attempt.getSchedule().getExam().getPassMark() != null) {
                attempt.setIsPassed(totalObtained >= attempt.getSchedule().getExam().getPassMark());
            }
        }
        attemptRepo.save(attempt);
    }

    /**
     * Parses a JSON-like string of UUIDs: ["uuid1","uuid2"] → List<UUID>
     * Handles null, empty, and malformed input gracefully.
     */
    public List<UUID> parseSelectedOptionIds(String json) {
        List<UUID> result = new ArrayList<>();
        if (json == null || json.isBlank()) return result;

        // Strip surrounding brackets and whitespace
        String stripped = json.trim();
        if (stripped.startsWith("[")) stripped = stripped.substring(1);
        if (stripped.endsWith("]")) stripped = stripped.substring(0, stripped.length() - 1);
        stripped = stripped.trim();

        if (stripped.isEmpty()) return result;

        String[] parts = stripped.split(",");
        for (String part : parts) {
            String cleaned = part.trim().replace("\"", "").replace("'", "").trim();
            if (!cleaned.isEmpty()) {
                try {
                    result.add(UUID.fromString(cleaned));
                } catch (IllegalArgumentException e) {
                    // skip malformed UUID
                }
            }
        }
        return result;
    }
}
