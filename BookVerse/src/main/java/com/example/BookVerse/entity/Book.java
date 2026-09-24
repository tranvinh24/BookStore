package com.example.BookVerse.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "book")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "title", "author", "isbn"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String title;
    private String author;
    private String isbn;
    private Integer year;
    private String category;
    private Double rating;

    /** Lưu dạng TEXT để hỗ trợ mô tả dài hơn 255 ký tự */
    @Column(columnDefinition = "TEXT")
    private String description;

    private String coverPath;
    private Long price;
    private Integer stock;
}
