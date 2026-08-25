package com.example.BookVerse.repository;

import com.example.BookVerse.entity.Order;
import com.example.BookVerse.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    // User
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<Order> findByIdAndUserId(String id, String userId);

    // Admin - lay tat ca don hang
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    // Admin - dem so don hang theo trang thai
    long countByStatus(OrderStatus status);

    // Admin - dem tong so don hang
    @Query("SELECT COUNT(o) FROM Order o")
    long countAllOrders();
}