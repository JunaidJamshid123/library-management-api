package com.library.library_management.service;

import com.library.library_management.model.BorrowStatus;
import com.library.library_management.repository.BookRepository;
import com.library.library_management.repository.BorrowRecordRepository;
import com.library.library_management.repository.FineRepository;
import com.library.library_management.repository.MemberRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class StatsService {

    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final FineRepository fineRepository;

    public StatsService(BookRepository bookRepository,
                        MemberRepository memberRepository,
                        BorrowRecordRepository borrowRecordRepository,
                        FineRepository fineRepository) {
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.fineRepository = fineRepository;
    }

    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalBooks", bookRepository.count());
        summary.put("totalMembers", memberRepository.count());
        summary.put("activeBorrows", borrowRecordRepository.countByStatus(BorrowStatus.BORROWED));
        summary.put("overdueCount", borrowRecordRepository.findByStatusAndDueDateBefore(
                BorrowStatus.BORROWED, LocalDate.now()).size());
        summary.put("totalFinesUnpaid", fineRepository.findByPaidFalse().size());
        return summary;
    }

    public List<Map<String, Object>> getTopBooks(int limit) {
        List<Object[]> results = borrowRecordRepository.findTopBorrowedBooks(PageRequest.of(0, limit));
        List<Map<String, Object>> topBooks = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("book", row[0]);
            entry.put("borrowCount", row[1]);
            topBooks.add(entry);
        }
        return topBooks;
    }

    public Map<String, Object> getOverdueRate() {
        long total = borrowRecordRepository.count();
        long overdue = borrowRecordRepository.countByStatus(BorrowStatus.OVERDUE);
        long currentlyOverdue = borrowRecordRepository.findByStatusAndDueDateBefore(
                BorrowStatus.BORROWED, LocalDate.now()).size();
        long totalOverdue = overdue + currentlyOverdue;

        double rate = total == 0 ? 0.0 : (double) totalOverdue / total * 100;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalBorrows", total);
        result.put("totalOverdue", totalOverdue);
        result.put("overdueRatePercent", Math.round(rate * 100.0) / 100.0);
        return result;
    }
}
