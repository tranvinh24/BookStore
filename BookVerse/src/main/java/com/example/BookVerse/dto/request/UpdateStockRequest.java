package com.example.BookVerse.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStockRequest {
    @NotNull(message = "So luong khong duoc de trong")
    @Min(value = 0, message = "So luong phai >= 0")
    private Integer stock;
}