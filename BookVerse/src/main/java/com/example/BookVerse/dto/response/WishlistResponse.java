package com.example.BookVerse.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistResponse {
    private String id;
    private String bookId;
    private String title;
    private String author;
    private String category;
    private Long price;
    private Integer stock;
    private String coverPath;
    private LocalDateTime addedAt;
}
