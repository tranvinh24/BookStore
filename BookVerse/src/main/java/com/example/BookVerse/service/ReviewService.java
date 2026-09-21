package com.example.BookVerse.service;

import com.example.BookVerse.dto.request.CreateReviewRequest;
import com.example.BookVerse.dto.response.ReviewResponse;
import com.example.BookVerse.entity.Book;
import com.example.BookVerse.entity.Review;
import com.example.BookVerse.entity.User;
import com.example.BookVerse.exception.AppException;
import com.example.BookVerse.exception.ErrorCode;
import com.example.BookVerse.repository.BookRepository;
import com.example.BookVerse.repository.ReviewRepository;
import com.example.BookVerse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository   bookRepository;
    private final UserRepository   userRepository;

    /**
     * Lấy danh sách bình luận của một cuốn sách.
     */
    public List<ReviewResponse> getReviewsByBook(String bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new AppException(ErrorCode.BOOK_NOT_FOUND);
        }

        List<Review> reviews = reviewRepository.findByBookIdOrderByCreatedAtDesc(bookId);
        return reviews.stream().map(this::toReviewResponse).toList();
    }

    /**
     * Thêm mới hoặc cập nhật đánh giá của user cho sách.
     * Tự động tính lại điểm rating trung bình của sách.
     */
    @Transactional
    public ReviewResponse addOrUpdateReview(String bookId, String userId, CreateReviewRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Review review = reviewRepository.findByBookIdAndUserId(bookId, userId)
                .orElse(null);

        if (review == null) {
            review = Review.builder()
                    .book(book)
                    .user(user)
                    .rating(request.getRating())
                    .comment(request.getComment().trim())
                    .build();
        } else {
            review.setRating(request.getRating());
            review.setComment(request.getComment().trim());
        }

        review = reviewRepository.save(review);

        // Tính lại điểm rating trung bình và cập nhật vào Book
        recalculateAndSaveBookRating(book);

        log.info("User {} đã đánh giá sách {} với {} sao", user.getUsername(), book.getTitle(), request.getRating());
        return toReviewResponse(review);
    }

    /**
     * Xóa đánh giá của sách (chỉ chủ sở hữu hoặc Admin).
     */
    @Transactional
    public void deleteReview(String reviewId, String userId, boolean isAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        if (!isAdmin && !review.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Book book = review.getBook();
        reviewRepository.delete(review);

        // Tính lại điểm rating sau khi xóa
        recalculateAndSaveBookRating(book);
        log.info("Đã xóa đánh giá id={} cho sách id={}", reviewId, book.getId());
    }

    /**
     * Tính toán rating trung bình từ tất cả review của sách và lưu lại.
     */
    private void recalculateAndSaveBookRating(Book book) {
        Double avgRating = reviewRepository.getAverageRatingByBookId(book.getId());
        if (avgRating != null) {
            double rounded = Math.round(avgRating * 10.0) / 10.0;
            book.setRating(rounded);
        } else {
            book.setRating(5.0); // Mặc định 5.0 nếu chưa có ai đánh giá
        }
        bookRepository.save(book);
    }

    private ReviewResponse toReviewResponse(Review review) {
        User user = review.getUser();
        String userName = user.getUsername();
        String userFullName = user.getFullName();
        String displayName = (userFullName != null && !userFullName.isBlank()) ? userFullName : userName;

        String avatarUrl = user.getAvatarPath();
        if (avatarUrl == null || avatarUrl.isBlank()) {
            String encodedName = URLEncoder.encode(displayName, StandardCharsets.UTF_8);
            avatarUrl = "https://ui-avatars.com/api/?name=" + encodedName + "&background=random&color=fff&size=128&bold=true";
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .bookId(review.getBook().getId())
                .userId(user.getId())
                .userName(userName)
                .userFullName(userFullName)
                .rating(review.getRating())
                .comment(review.getComment())
                .avatarUrl(avatarUrl)
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
