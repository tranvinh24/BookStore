package com.example.BookVerse.controller;

import com.example.BookVerse.dto.request.LoginRequest;
import com.example.BookVerse.dto.request.RegisterRequest;
import com.example.BookVerse.dto.response.AuthResponse;
import com.example.BookVerse.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller xử lý xác thực (đăng ký, đăng nhập).
 * Base path: /api/auth — toàn bộ public, không cần token.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Đăng ký tài khoản mới.
     *
     * @param request userName, password, email, sdt (tùy chọn)
     * @return JWT token + thông tin user
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * POST /api/auth/login
     * Đăng nhập và lấy JWT token.
     *
     * @param request userName, password
     * @return JWT token + thông tin user
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
