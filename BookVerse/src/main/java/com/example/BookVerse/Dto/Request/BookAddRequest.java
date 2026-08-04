package com.example.BookVerse.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookAddRequest {
    String id;
    String name;
    String author;
    String genre;
}
