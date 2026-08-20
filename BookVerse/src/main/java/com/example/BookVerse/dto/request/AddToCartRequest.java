package com.example.BookVerse.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddToCartRequest {
    @NotBlank(message = "bookId khong duoc de trong")
    private String bookId;

    @NotNull(message = "So luong khong duoc de trong")
    @Min(value = 1, message = "So luong toi thieu la 1")
    private Integer quantity;
}
