package com.library.library_management.service;

import com.library.library_management.exception.IllegalOperationException;
import com.library.library_management.exception.ResourceNotFoundException;
import com.library.library_management.model.*;
import com.library.library_management.repository.BookRepository;
import com.library.library_management.repository.BorrowRecordRepository;
import com.library.library_management.repository.FineRepository;
import com.library.library_management.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BorrowService {

    private static final double FINE_PER_DAY = 1.0;
    private static final int BORROW_PERIOD_DAYS = 14;
    private static final int EXTENSION_DAYS = 7;

    private final BorrowRecordRepository borrowRecordRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final FineRepository fineRepository;

    public BorrowService(BorrowRecordRepository borrowRecordRepository,
                         BookRepository bookRepository,
                         MemberRepository memberRepository,
                         FineRepository fineRepository) {
        this.borrowRecordRepository = borrowRecordRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.fineRepository = fineRepository;
    }

    @Transactional
    public BorrowRecord borrowBook(Long bookId, Long memberId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + memberId));

        if (!member.isActive()) {
            throw new IllegalOperationException("Member account is inactive");
        }
        if (book.getAvailableCopies() <= 0) {
            throw new IllegalOperationException("No available copies of this book");
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        BorrowRecord record = BorrowRecord.builder()
                .book(book)
                .member(member)
                .borrowDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(BORROW_PERIOD_DAYS))
                .status(BorrowStatus.BORROWED)
                .build();

        return borrowRecordRepository.save(record);
    }

    @Transactional
    public BorrowRecord returnBook(Long borrowId) {
        BorrowRecord record = borrowRecordRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrow record not found with id: " + borrowId));

        if (record.getStatus() == BorrowStatus.RETURNED) {
            throw new IllegalOperationException("Book has already been returned");
        }

        record.setReturnDate(LocalDate.now());
        record.setStatus(BorrowStatus.RETURNED);

        Book book = record.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        // Generate fine if overdue
        if (LocalDate.now().isAfter(record.getDueDate())) {
            long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), LocalDate.now());
            Fine fine = Fine.builder()
                    .borrowRecord(record)
                    .amount(overdueDays * FINE_PER_DAY)
                    .paid(false)
                    .issuedDate(LocalDate.now())
                    .build();
            fineRepository.save(fine);
        }

        return borrowRecordRepository.save(record);
    }

    public List<BorrowRecord> getOverdueRecords() {
        return borrowRecordRepository.findByStatusAndDueDateBefore(BorrowStatus.BORROWED, LocalDate.now());
    }

    public List<BorrowRecord> getActiveRecords() {
        return borrowRecordRepository.findByStatus(BorrowStatus.BORROWED);
    }

    @Transactional
    public BorrowRecord extendBorrow(Long borrowId) {
        BorrowRecord record = borrowRecordRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrow record not found with id: " + borrowId));

        if (record.getStatus() != BorrowStatus.BORROWED) {
            throw new IllegalOperationException("Cannot extend a borrow that is not active");
        }

        if (record.getMember().getMembershipType() != MembershipType.PREMIUM) {
            throw new IllegalOperationException("Only PREMIUM members can extend borrows");
        }

        record.setDueDate(record.getDueDate().plusDays(EXTENSION_DAYS));
        return borrowRecordRepository.save(record);
    }

    public List<BorrowRecord> getBorrowHistoryByMember(Long memberId) {
        return borrowRecordRepository.findByMemberId(memberId);
    }
}
