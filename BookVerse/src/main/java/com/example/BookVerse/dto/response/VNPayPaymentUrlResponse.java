package com.example.BookVerse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayPaymentUrlResponse {
    private String orderId;
    private Long amount;
    private String paymentUrl;
}
