package com.example.BookVerse.controller;

import com.example.BookVerse.dto.request.AddToWishlistRequest;
import com.example.BookVerse.dto.response.WishlistResponse;
import com.example.BookVerse.entity.User;
import com.example.BookVerse.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API quản lý danh sách yêu thích.
 * Tất cả endpoint yêu cầu xác thực (ROLE_USER).
 */
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    /** GET /api/wishlist — Lấy danh sách yêu thích của user hiện tại */
    @GetMapping
    public List<WishlistResponse> getWishlist(@AuthenticationPrincipal User user) {
        return wishlistService.getWishlist(user.getId());
    }

    /** POST /api/wishlist — Thêm sách vào danh sách yêu thích */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WishlistResponse addToWishlist(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddToWishlistRequest request) {
        return wishlistService.addToWishlist(user.getId(), request);
    }

    /** DELETE /api/wishlist/{bookId} — Xóa sách khỏi danh sách yêu thích */
    @DeleteMapping("/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromWishlist(
            @AuthenticationPrincipal User user,
            @PathVariable String bookId) {
        wishlistService.removeFromWishlist(user.getId(), bookId);
    }
}
