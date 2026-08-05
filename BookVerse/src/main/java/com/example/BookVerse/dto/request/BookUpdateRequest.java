package com.example.BookVerse.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookUpdateRequest {

    private String title;
    private String author;
    private String isbn;
    private Integer year;
    private String category;
    private Double rating;
    private String description;
    private String coverPath;
}
