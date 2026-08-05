package com.example.BookVerse.Dto.Request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookCreateRequest {
    private String id;
    @NotBlank(message = "Khong duoc de trong")
    @Size(max =200)
    private String title;
    @NotBlank(message = "Khong duoc de trong")
    @Size(max =200)
    private String author;
    @Pattern(
            regexp = "^(97[89])-\\d-\\d{2,5}-\\d{2,7}-\\d$",
            message = "ISBN-13 không hợp lệ"
    )
    @Size(min=10, max = 17)
    private String isbn;
    @NotNull
    @Min(1900)
    @Max(2100)
    private Integer year;
    @NotBlank(message = "Khong duoc de trong")
    private String category;
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("5.0")
    private Double rating;
    @Size(max = 2000)
    private String description;
    @Size(max = 255)
    private String coverPath;
}
