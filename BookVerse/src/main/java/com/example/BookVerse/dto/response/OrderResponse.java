package com.example.BookVerse.dto.response;

import com.example.BookVerse.enums.OrderStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String id;
    private OrderStatus status;
    private Long totalAmount;
    private String shippingAddress;
    private String trackingNote;
    private List<OrderItemResponse> items;
    private PaymentResponse payment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
