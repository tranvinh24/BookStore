package com.example.BookVerse.repository;

import com.example.BookVerse.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, String> {

    List<Review> findByBookIdOrderByCreatedAtDesc(String bookId);

    Optional<Review> findByBookIdAndUserId(String bookId, String userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.book.id = :bookId")
    Double getAverageRatingByBookId(@Param("bookId") String bookId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.book.id = :bookId")
    Long countByBookId(@Param("bookId") String bookId);
}
