package com.edulearn.dto.response;

import java.util.List;

public record StudentDashboardResponse(
        long availableExams,
        long examsTaken,
        Double averagePercentage,
        Double passRate,
        List<StudentExamCardResponse> upcomingExams,
        List<StudentExamCardResponse> recentResults
) {}
