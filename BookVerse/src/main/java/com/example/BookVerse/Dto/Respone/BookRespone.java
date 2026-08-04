package com.example.BookVerse.Dto.Respone;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookRespone {
    String id;
    String name;
    String author;
    String genre;
}
