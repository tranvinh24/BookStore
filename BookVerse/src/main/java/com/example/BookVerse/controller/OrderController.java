package com.example.BookVerse.controller;

import com.example.BookVerse.dto.request.CheckoutRequest;
import com.example.BookVerse.dto.response.OrderResponse;
import com.example.BookVerse.entity.User;
import com.example.BookVerse.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API đặt hàng và theo dõi đơn hàng.
 * Tất cả endpoint yêu cầu xác thực (ROLE_USER).
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** POST /api/orders/checkout — Đặt hàng từ giỏ hàng hiện tại */
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(user.getId(), request);
    }

    /** GET /api/orders/my — Lấy danh sách đơn hàng của user */
    @GetMapping("/my")
    public List<OrderResponse> getMyOrders(@AuthenticationPrincipal User user) {
        return orderService.getMyOrders(user.getId());
    }

    /** GET /api/orders/{id} — Chi tiết 1 đơn hàng (chỉ đơn của chính mình) */
    @GetMapping("/{id}")
    public OrderResponse getOrderDetail(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        return orderService.getOrderDetail(user.getId(), id);
    }
}
