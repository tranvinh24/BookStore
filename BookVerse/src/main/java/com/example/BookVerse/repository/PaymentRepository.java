package com.example.BookVerse.repository;

import com.example.BookVerse.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findByOrderIdAndOrderUserId(String orderId, String userId);
}
