package com.library.library_management.controller;

import com.library.library_management.dto.ApiResponse;
import com.library.library_management.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary() {
        Map<String, Object> summary = statsService.getSummary();
        return ResponseEntity.ok(
                ApiResponse.success(200, "Summary fetched successfully", summary)
        );
    }

    @GetMapping("/top-books")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopBooks(
            @RequestParam(defaultValue = "5") int limit) {
        List<Map<String, Object>> topBooks = statsService.getTopBooks(limit);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Top borrowed books fetched successfully", topBooks)
        );
    }

    @GetMapping("/overdue-rate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverdueRate() {
        Map<String, Object> rate = statsService.getOverdueRate();
        return ResponseEntity.ok(
                ApiResponse.success(200, "Overdue rate fetched successfully", rate)
        );
    }
}
