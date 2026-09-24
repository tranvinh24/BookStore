package com.example.BookVerse.repository;

import com.example.BookVerse.entity.Order;
import com.example.BookVerse.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {

    // ── User endpoints ─────────────────────────────────────────────────────────

    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Order> findByIdAndUserId(String id, String userId);

    // ── Admin: phân trang + JOIN FETCH để tránh N+1 ────────────────────────────

    /**
     * Lấy tất cả đơn hàng có phân trang.
     * JOIN FETCH user và payment trong 1 query → loại bỏ N+1 khi gọi toOrderSummary().
     *
     * Cần countQuery riêng vì DISTINCT + JOIN không đếm được chuẩn.
     */
    @Query(
        value = """
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.user
            LEFT JOIN FETCH o.payment
            ORDER BY o.createdAt DESC
            """,
        countQuery = "SELECT COUNT(o) FROM Order o"
    )
    Page<Order> findAllWithUserAndPayment(Pageable pageable);

    /**
     * Lấy đơn hàng theo trạng thái có phân trang.
     * JOIN FETCH user và payment trong 1 query.
     */
    @Query(
        value = """
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.user
            LEFT JOIN FETCH o.payment
            WHERE o.status = :status
            ORDER BY o.createdAt DESC
            """,
        countQuery = "SELECT COUNT(o) FROM Order o WHERE o.status = :status"
    )
    Page<Order> findByStatusWithUserAndPayment(@Param("status") OrderStatus status, Pageable pageable);

    // ── Admin: thống kê ────────────────────────────────────────────────────────

    long countByStatus(OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o")
    long countAllOrders();
}