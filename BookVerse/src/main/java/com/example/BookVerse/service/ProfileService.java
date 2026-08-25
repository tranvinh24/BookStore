package com.example.BookVerse.service;

import com.example.BookVerse.dto.request.UpdateProfileRequest;
import com.example.BookVerse.dto.response.UserResponse;
import com.example.BookVerse.entity.User;
import com.example.BookVerse.exception.AppException;
import com.example.BookVerse.exception.ErrorCode;
import com.example.BookVerse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ProfileService xu ly viec xem va cap nhat thong tin ca nhan user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** Lay thong tin ca nhan user hien tai */
    public UserResponse getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return toUserResponse(user);
    }

    /** Cap nhat email, sdt, mat khau */
    @Transactional
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Cap nhat email neu co
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!request.getEmail().equals(user.getEmail())
                    && userRepository.existsByEmail(request.getEmail())) {
                throw new AppException(ErrorCode.EMAIL_EXISTED);
            }
            user.setEmail(request.getEmail());
        }

        // Cap nhat so dien thoai
        if (request.getSdt() != null) {
            user.setSdt(request.getSdt());
        }

        // Doi mat khau neu co yeu cau
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new AppException(ErrorCode.CURRENT_PASSWORD_REQUIRED);
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new AppException(ErrorCode.INVALID_CREDENTIALS);
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            log.info("User {} doi mat khau thanh cong", userId);
        }

        return toUserResponse(userRepository.save(user));
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .userName(user.getUsername())
                .email(user.getEmail())
                .sdt(user.getSdt())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}