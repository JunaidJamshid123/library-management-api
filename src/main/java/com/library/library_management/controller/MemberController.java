package com.library.library_management.controller;

import com.library.library_management.dto.ApiResponse;
import com.library.library_management.model.Member;
import com.library.library_management.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
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
            @RequestParam(required = false) String name) {

        List<Member> members = memberService.searchMembers(name);
        if (members.isEmpty()) {
            String msg = name != null
                    ? "No members found with name '" + name + "'"
                    : "No members found";
            return ResponseEntity.ok(ApiResponse.success(200, msg, members));
        }
        return ResponseEntity.ok(
                ApiResponse.success(200, "Members fetched successfully", members)
        );
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

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Member deleted successfully", null)
        );
    }
}
