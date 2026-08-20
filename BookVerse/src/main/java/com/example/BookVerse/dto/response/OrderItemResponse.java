package com.example.BookVerse.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private String id;
    private String bookId;
    private String title;
    private String author;
    private Integer quantity;
    private Long priceAtOrder;
    private Long subtotal;
}
