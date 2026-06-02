package com.edulearn.controller;

import com.edulearn.service.DemoDataService;
import com.edulearn.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/demo")
@PreAuthorize("hasRole('ADMIN')")
public class DemoDataController {

    private final DemoDataService demoDataService;

    public DemoDataController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> stats() {
        return ResponseEntity.ok(ApiResponse.success("Stats loaded", demoDataService.getStats()));
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> generate(@RequestBody Map<String, Integer> body) {
        int cats    = Math.max(1, Math.min(body.getOrDefault("numCategories",      3), 6));
        int topics  = Math.max(1, Math.min(body.getOrDefault("topicsPerSubject",   3), 10));
        int qCount  = Math.max(1, Math.min(body.getOrDefault("questionsPerTopic",  5), 20));
        Map<String, Integer> result = demoDataService.generateDemoData(cats, topics, qCount);
        return ResponseEntity.ok(ApiResponse.success("Demo data generated successfully", result));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> clear() {
        Map<String, Integer> result = demoDataService.clearDemoData();
        return ResponseEntity.ok(ApiResponse.success("All demo data cleared. Users and organisation settings preserved.", result));
    }
}
