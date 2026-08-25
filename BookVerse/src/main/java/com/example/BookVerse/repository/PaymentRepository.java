package com.example.BookVerse.repository;

import com.example.BookVerse.entity.Payment;
import com.example.BookVerse.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findByOrderIdAndOrderUserId(String orderId, String userId);

    // Thong ke doanh thu theo khoang thoi gian
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status AND p.paidAt BETWEEN :from AND :to")
    Long sumAmountByStatusAndPaidAtBetween(
        @Param("status") PaymentStatus status,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    // Dem so luong thanh toan thanh cong theo khoang thoi gian
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status AND p.paidAt BETWEEN :from AND :to")
    Long countByStatusAndPaidAtBetween(
        @Param("status") PaymentStatus status,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
}