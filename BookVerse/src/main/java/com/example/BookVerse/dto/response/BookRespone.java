package com.example.BookVerse.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookRespone {
    private String id;
    private String title;
    private String author;
    private String isbn;
    private Integer year;
    private String category;
    private Double rating;
    private String description;
    private String coverPath;
}
