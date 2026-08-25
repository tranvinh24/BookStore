package com.example.BookVerse.controller;

import com.example.BookVerse.dto.request.UpdateProfileRequest;
import com.example.BookVerse.dto.response.UserResponse;
import com.example.BookVerse.entity.User;
import com.example.BookVerse.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * API quan ly thong tin ca nhan user.
 * Tat ca endpoint yeu cau xac thuc (bat ky role nao).
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * GET /api/profile
     * Lay thong tin ca nhan cua user dang dang nhap.
     */
    @GetMapping
    public UserResponse getProfile(@AuthenticationPrincipal User user) {
        return profileService.getProfile(user.getId());
    }

    /**
     * PUT /api/profile
     * Cap nhat email, sdt, hoac doi mat khau.
     * Yeu cau currentPassword khi doi mat khau moi.
     */
    @PutMapping
    public UserResponse updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(user.getId(), request);
    }
}