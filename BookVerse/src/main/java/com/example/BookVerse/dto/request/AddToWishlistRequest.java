package com.example.BookVerse.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddToWishlistRequest {
    @NotBlank(message = "bookId khong duoc de trong")
    private String bookId;
}
