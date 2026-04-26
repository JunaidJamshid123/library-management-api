package com.library.library_management.repository;

import com.library.library_management.model.Member;
import com.library.library_management.model.MembershipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    List<Member> findByNameContainingIgnoreCase(String name);

    List<Member> findByMembershipType(MembershipType membershipType);
}
