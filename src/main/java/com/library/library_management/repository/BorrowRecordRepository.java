package com.library.library_management.repository;

import com.library.library_management.model.BorrowRecord;
import com.library.library_management.model.BorrowStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    List<BorrowRecord> findByStatus(BorrowStatus status);

    List<BorrowRecord> findByStatusAndDueDateBefore(BorrowStatus status, LocalDate date);

    List<BorrowRecord> findByMemberId(Long memberId);

    long countByStatus(BorrowStatus status);

    @Query("SELECT br.book, COUNT(br) AS cnt FROM BorrowRecord br GROUP BY br.book ORDER BY cnt DESC")
    List<Object[]> findTopBorrowedBooks(Pageable pageable);
}
