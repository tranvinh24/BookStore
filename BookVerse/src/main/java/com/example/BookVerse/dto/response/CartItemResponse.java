package com.example.BookVerse.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private String cartItemId;
    private String bookId;
    private String title;
    private String author;
    private Long price;
    private Integer quantity;
    private Long subtotal;
    private Integer availableStock;
}
