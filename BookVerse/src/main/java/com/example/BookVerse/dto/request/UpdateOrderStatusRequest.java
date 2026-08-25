package com.example.BookVerse.dto.request;

import com.example.BookVerse.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {
    @NotNull(message = "Trang thai don hang khong duoc de trong")
    private OrderStatus status;

    private String trackingNote;
}