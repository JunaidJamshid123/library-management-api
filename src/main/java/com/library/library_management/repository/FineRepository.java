package com.library.library_management.repository;

import com.library.library_management.model.Fine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FineRepository extends JpaRepository<Fine, Long> {

    List<Fine> findByPaidFalse();

    List<Fine> findByBorrowRecordMemberId(Long memberId);
}
