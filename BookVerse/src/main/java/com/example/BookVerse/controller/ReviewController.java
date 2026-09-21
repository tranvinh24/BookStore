package com.example.BookVerse.controller;

import com.example.BookVerse.dto.request.CreateReviewRequest;
import com.example.BookVerse.dto.response.ReviewResponse;
import com.example.BookVerse.entity.User;
import com.example.BookVerse.enums.Role;
import com.example.BookVerse.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * GET /api/books/{bookId}/reviews
     * Lấy danh sách bình luận của cuốn sách (Public).
     */
    @GetMapping("/api/books/{bookId}/reviews")
    public List<ReviewResponse> getReviews(@PathVariable String bookId) {
        return reviewService.getReviewsByBook(bookId);
    }

    /**
     * POST /api/books/{bookId}/reviews
     * Gửi / cập nhật bình luận và đánh giá sao cho sách (Authenticated).
     */
    @PostMapping("/api/books/{bookId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse addReview(
            @PathVariable String bookId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateReviewRequest request) {
        return reviewService.addOrUpdateReview(bookId, user.getId(), request);
    }

    /**
     * DELETE /api/reviews/{id}
     * Xóa bình luận (Chính chủ hoặc Admin).
     */
    @DeleteMapping("/api/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        boolean isAdmin = user.getRole() == Role.ADMIN;
        reviewService.deleteReview(id, user.getId(), isAdmin);
    }
}
