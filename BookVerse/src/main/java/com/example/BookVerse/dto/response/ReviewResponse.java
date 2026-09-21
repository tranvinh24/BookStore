package com.example.BookVerse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private String id;
    private String bookId;
    private String userId;
    private String userName;
    private String userFullName;
    private Integer rating;
    private String comment;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
