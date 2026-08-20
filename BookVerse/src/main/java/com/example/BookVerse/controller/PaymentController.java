package com.example.BookVerse.controller;

import com.example.BookVerse.dto.response.PaymentResponse;
import com.example.BookVerse.entity.User;
import com.example.BookVerse.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * API thanh toán đơn hàng.
 * Tất cả endpoint yêu cầu xác thực (ROLE_USER).
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** POST /api/payments/{orderId}/pay — Giả lập thanh toán đơn hàng */
    @PostMapping("/{orderId}/pay")
    public PaymentResponse processPayment(
            @AuthenticationPrincipal User user,
            @PathVariable String orderId) {
        return paymentService.processPayment(user.getId(), orderId);
    }

    /** GET /api/payments/{orderId} — Lấy thông tin thanh toán của đơn hàng */
    @GetMapping("/{orderId}")
    public PaymentResponse getPayment(
            @AuthenticationPrincipal User user,
            @PathVariable String orderId) {
        return paymentService.getPaymentByOrder(user.getId(), orderId);
    }
}
