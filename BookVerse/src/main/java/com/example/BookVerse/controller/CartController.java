package com.example.BookVerse.controller;

import com.example.BookVerse.dto.request.AddToCartRequest;
import com.example.BookVerse.dto.request.UpdateCartItemRequest;
import com.example.BookVerse.dto.response.CartResponse;
import com.example.BookVerse.entity.User;
import com.example.BookVerse.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * API quản lý giỏ hàng.
 * Tất cả endpoint yêu cầu xác thực (ROLE_USER).
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /** GET /api/cart — Lấy giỏ hàng hiện tại */
    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal User user) {
        return cartService.getCart(user.getId());
    }

    /** POST /api/cart — Thêm sách vào giỏ hàng (có kiểm tra tồn kho) */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse addToCart(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddToCartRequest request) {
        return cartService.addToCart(user.getId(), request);
    }

    /** PUT /api/cart/{cartItemId} — Cập nhật số lượng */
    @PutMapping("/{cartItemId}")
    public CartResponse updateQuantity(
            @AuthenticationPrincipal User user,
            @PathVariable String cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateQuantity(user.getId(), cartItemId, request);
    }

    /** DELETE /api/cart/{cartItemId} — Xóa 1 sản phẩm khỏi giỏ */
    @DeleteMapping("/{cartItemId}")
    public CartResponse removeFromCart(
            @AuthenticationPrincipal User user,
            @PathVariable String cartItemId) {
        return cartService.removeFromCart(user.getId(), cartItemId);
    }

    /** DELETE /api/cart — Xóa toàn bộ giỏ hàng */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(@AuthenticationPrincipal User user) {
        cartService.clearCart(user.getId());
    }
}
