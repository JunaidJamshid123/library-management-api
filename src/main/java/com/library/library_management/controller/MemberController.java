package com.library.library_management.controller;

import com.library.library_management.dto.ApiResponse;
import com.library.library_management.model.BorrowRecord;
import com.library.library_management.model.Fine;
import com.library.library_management.model.Member;
import com.library.library_management.model.MembershipType;
import com.library.library_management.service.BorrowService;
import com.library.library_management.service.FineService;
import com.library.library_management.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;
    private final BorrowService borrowService;
    private final FineService fineService;

    public MemberController(MemberService memberService, BorrowService borrowService, FineService fineService) {
        this.memberService = memberService;
        this.borrowService = borrowService;
        this.fineService = fineService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Member>> createMember(@RequestBody Member member) {
        Member created = memberService.createMember(member);
        return new ResponseEntity<>(
                ApiResponse.success(201, "Member created successfully", created),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Member>>> getAllMembers(
            @RequestParam(required = false) MembershipType type) {

        List<Member> members = memberService.searchMembers(null, type);
        String msg = members.isEmpty() ? "No members found" : "Members fetched successfully";
        return ResponseEntity.ok(ApiResponse.success(200, msg, members));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Member>> getMemberById(@PathVariable Long id) {
        Member member = memberService.getMemberById(id);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Member fetched successfully", member)
        );
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<Member>> getMemberByEmail(@PathVariable String email) {
        Member member = memberService.getMemberByEmail(email);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Member fetched successfully", member)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Member>> updateMember(
            @PathVariable Long id,
            @RequestBody Member memberDetails) {
        Member updated = memberService.updateMember(id, memberDetails);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Member updated successfully", updated)
        );
    }

    @PatchMapping("/{id}/upgrade")
    public ResponseEntity<ApiResponse<Member>> upgradeMembership(@PathVariable Long id) {
        Member upgraded = memberService.upgradeMembership(id);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Membership upgraded to PREMIUM", upgraded)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Member deactivated successfully", null)
        );
    }

    @GetMapping("/{id}/borrow-history")
    public ResponseEntity<ApiResponse<List<BorrowRecord>>> getBorrowHistory(@PathVariable Long id) {
        memberService.getMemberById(id);
        List<BorrowRecord> history = borrowService.getBorrowHistoryByMember(id);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Borrow history fetched successfully", history)
        );
    }

    @GetMapping("/{id}/fines")
    public ResponseEntity<ApiResponse<List<Fine>>> getMemberFines(@PathVariable Long id) {
        memberService.getMemberById(id);
        List<Fine> fines = fineService.getFinesByMember(id);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Fines fetched successfully", fines)
        );
    }
}
