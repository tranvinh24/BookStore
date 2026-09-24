package com.example.BookVerse.dto.response;

import com.example.BookVerse.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayCallbackResponse {
    private String code;            // "00" = thành công
    private String message;
    private String orderId;
    private String transactionId;
    private String bankCode;
    private Long amount;
    private PaymentStatus paymentStatus;
    private LocalDateTime paidAt;
}
