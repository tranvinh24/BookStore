package com.example.BookVerse.dto.response;

import com.example.BookVerse.enums.OrderStatus;
import com.example.BookVerse.enums.PaymentStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponse {
    private String id;
    private String userId;
    private String userName;
    private OrderStatus status;
    private Long totalAmount;
    private String shippingAddress;
    private String trackingNote;
    private PaymentStatus paymentStatus;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int itemCount;
}