package com.library.library_management.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRequest {
    private Long bookId;
    private Long memberId;
}
