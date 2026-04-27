package com.library.library_management.service;

import com.library.library_management.exception.DuplicateResourceException;
import com.library.library_management.exception.IllegalOperationException;
import com.library.library_management.exception.ResourceNotFoundException;
import com.library.library_management.model.Member;
import com.library.library_management.model.MembershipType;
import com.library.library_management.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
    public Member createMember(Member member) {
        Optional<Member> existing = memberRepository.findByEmail(member.getEmail());
        if (existing.isPresent()) {
            throw new DuplicateResourceException("Member with email '" + member.getEmail() + "' already exists");
        }
        member.setActive(true);
        return memberRepository.save(member);
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    public Member getMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + id));
    }

    public Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with email: " + email));
    }

    public List<Member> searchMembers(String name, MembershipType type) {
        if (type != null) {
            return memberRepository.findByMembershipType(type);
        }
        if (name != null) {
            return memberRepository.findByNameContainingIgnoreCase(name);
        }
        return memberRepository.findAll();
    }

    public Member updateMember(Long id, Member memberDetails) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + id));

        member.setName(memberDetails.getName());
        member.setEmail(memberDetails.getEmail());
        member.setPhone(memberDetails.getPhone());
        member.setMembershipType(memberDetails.getMembershipType());
        member.setMembershipDate(memberDetails.getMembershipDate());
        member.setActive(memberDetails.isActive());

        return memberRepository.save(member);
    }

    public Member upgradeMembership(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + id));
        if (member.getMembershipType() == MembershipType.PREMIUM) {
            throw new IllegalOperationException("Member is already PREMIUM");
        }
        member.setMembershipType(MembershipType.PREMIUM);
        return memberRepository.save(member);
    }

    public void deleteMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + id));
        member.setActive(false);
        memberRepository.save(member);
    }
}
