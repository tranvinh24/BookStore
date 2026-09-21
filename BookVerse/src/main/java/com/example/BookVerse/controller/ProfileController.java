package com.example.BookVerse.controller;

import com.example.BookVerse.dto.request.ChangePasswordRequest;
import com.example.BookVerse.dto.request.UpdateProfileRequest;
import com.example.BookVerse.dto.response.UserResponse;
import com.example.BookVerse.entity.User;
import com.example.BookVerse.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /** GET /api/profile */
    @GetMapping
    public UserResponse getProfile(@AuthenticationPrincipal User user) {
        return profileService.getProfile(user.getId());
    }

    /** PUT /api/profile */
    @PutMapping
    public UserResponse updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(user.getId(), request);
    }

    /** POST /api/profile/change-password */
    @PostMapping("/change-password")
    public void changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(user.getId(), request);
    }

    /** POST /api/profile/avatar — Upload ảnh đại diện (multipart) */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse uploadAvatar(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) throws IOException {
        return profileService.uploadAvatar(user.getId(), file);
    }
}