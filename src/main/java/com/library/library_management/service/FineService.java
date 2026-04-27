package com.library.library_management.service;

import com.library.library_management.exception.IllegalOperationException;
import com.library.library_management.exception.ResourceNotFoundException;
import com.library.library_management.model.Fine;
import com.library.library_management.repository.FineRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class FineService {

    private final FineRepository fineRepository;

    public FineService(FineRepository fineRepository) {
        this.fineRepository = fineRepository;
    }

    public List<Fine> getUnpaidFines() {
        return fineRepository.findByPaidFalse();
    }

    public Fine payFine(Long fineId) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine not found with id: " + fineId));

        if (fine.isPaid()) {
            throw new IllegalOperationException("Fine has already been paid");
        }

        fine.setPaid(true);
        fine.setPaidDate(LocalDate.now());
        return fineRepository.save(fine);
    }

    public List<Fine> getFinesByMember(Long memberId) {
        return fineRepository.findByBorrowRecordMemberId(memberId);
    }
}
