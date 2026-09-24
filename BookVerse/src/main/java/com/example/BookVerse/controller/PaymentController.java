package com.example.BookVerse.controller;

import com.example.BookVerse.dto.response.PaymentResponse;
import com.example.BookVerse.dto.response.VNPayCallbackResponse;
import com.example.BookVerse.dto.response.VNPayPaymentUrlResponse;
import com.example.BookVerse.entity.User;
import com.example.BookVerse.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API thanh toán đơn hàng & Tích hợp Cổng thanh toán VNPay Sandbox.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payments/{orderId}/vnpay-url
     * Lấy URL thanh toán VNPay Sandbox cho đơn hàng.
     */
    @PostMapping("/{orderId}/vnpay-url")
    public VNPayPaymentUrlResponse getVNPayPaymentUrl(
            @AuthenticationPrincipal User user,
            @PathVariable String orderId,
            HttpServletRequest request) {
        return paymentService.createVNPayPaymentUrl(user.getId(), orderId, request);
    }

    /**
     * GET /api/payments/vnpay-callback
     * Xác thực kết quả thanh toán từ VNPay return URL.
     * Endpoint này public (không yêu cầu JWT header) để frontend hoặc webhook gọi trực tiếp.
     */
    @GetMapping("/vnpay-callback")
    public VNPayCallbackResponse processVNPayCallback(
            @RequestParam Map<String, String> allParams) {
        return paymentService.processVNPayCallback(allParams);
    }

    /**
     * POST /api/payments/{orderId}/pay
     * Giả lập thanh toán nhanh đơn hàng.
     */
    @PostMapping("/{orderId}/pay")
    public PaymentResponse processPayment(
            @AuthenticationPrincipal User user,
            @PathVariable String orderId) {
        return paymentService.processPayment(user.getId(), orderId);
    }

    /**
     * GET /api/payments/{orderId}
     * Lấy thông tin thanh toán của đơn hàng.
     */
    @GetMapping("/{orderId}")
    public PaymentResponse getPayment(
            @AuthenticationPrincipal User user,
            @PathVariable String orderId) {
        return paymentService.getPaymentByOrder(user.getId(), orderId);
    }
}
