package com.library.library_management.controller;

import com.library.library_management.dto.ApiResponse;
import com.library.library_management.dto.BorrowRequest;
import com.library.library_management.model.BorrowRecord;
import com.library.library_management.service.BorrowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/borrow")
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BorrowRecord>> borrowBook(@RequestBody BorrowRequest request) {
        BorrowRecord record = borrowService.borrowBook(request.getBookId(), request.getMemberId());
        return new ResponseEntity<>(
                ApiResponse.success(201, "Book borrowed successfully", record),
                HttpStatus.CREATED
        );
    }

    @PatchMapping("/{id}/return")
    public ResponseEntity<ApiResponse<BorrowRecord>> returnBook(@PathVariable Long id) {
        BorrowRecord record = borrowService.returnBook(id);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Book returned successfully", record)
        );
    }

    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<List<BorrowRecord>>> getOverdueRecords() {
        List<BorrowRecord> records = borrowService.getOverdueRecords();
        return ResponseEntity.ok(
                ApiResponse.success(200, "Overdue records fetched successfully", records)
        );
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<BorrowRecord>>> getActiveRecords() {
        List<BorrowRecord> records = borrowService.getActiveRecords();
        return ResponseEntity.ok(
                ApiResponse.success(200, "Active borrow records fetched successfully", records)
        );
    }

    @PatchMapping("/{id}/extend")
    public ResponseEntity<ApiResponse<BorrowRecord>> extendBorrow(@PathVariable Long id) {
        BorrowRecord record = borrowService.extendBorrow(id);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Borrow extended successfully", record)
        );
    }
}
