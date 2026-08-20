package com.example.BookVerse.dto.response;

import com.example.BookVerse.enums.PaymentMethod;
import com.example.BookVerse.enums.PaymentStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String id;
    private PaymentMethod method;
    private PaymentStatus status;
    private Long amount;
    private String transactionId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
