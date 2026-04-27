package com.library.library_management.controller;

import com.library.library_management.dto.ApiResponse;
import com.library.library_management.model.Fine;
import com.library.library_management.service.FineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fines")
public class FineController {

    private final FineService fineService;

    public FineController(FineService fineService) {
        this.fineService = fineService;
    }

    @GetMapping("/unpaid")
    public ResponseEntity<ApiResponse<List<Fine>>> getUnpaidFines() {
        List<Fine> fines = fineService.getUnpaidFines();
        return ResponseEntity.ok(
                ApiResponse.success(200, "Unpaid fines fetched successfully", fines)
        );
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<Fine>> payFine(@PathVariable Long id) {
        Fine fine = fineService.payFine(id);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Fine paid successfully", fine)
        );
    }
}
