package com.example.BookVerse.service;

import com.example.BookVerse.config.JwtUtil;
import com.example.BookVerse.dto.request.LoginRequest;
import com.example.BookVerse.dto.request.RegisterRequest;
import com.example.BookVerse.dto.response.AuthResponse;
import com.example.BookVerse.entity.User;
import com.example.BookVerse.enums.Role;
import com.example.BookVerse.exception.AppException;
import com.example.BookVerse.exception.ErrorCode;
import com.example.BookVerse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service xử lý đăng ký và đăng nhập.
 * UserDetailsService được tách sang UserDetailsServiceImpl để tránh circular dependency.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil         jwtUtil;

    // ──────────────────────────────────────────────
    // Đăng ký
    // ──────────────────────────────────────────────

    /**
     * Đăng ký tài khoản mới với role USER.
     *
     * @throws AppException nếu userName hoặc email đã tồn tại
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        User user = User.builder()
                .userName(request.getUserName())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .sdt(request.getSdt())
                .role(Role.USER)
                .build();

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user);
        return buildAuthResponse(token, user);
    }

    // ──────────────────────────────────────────────
    // Đăng nhập
    // ──────────────────────────────────────────────

    /**
     * Đăng nhập bằng userName + password.
     *
     * @throws AppException nếu thông tin sai
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUserName(request.getUserName())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        String token = jwtUtil.generateToken(user);
        return buildAuthResponse(token, user);
    }

    // ──────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .userName(user.getUsername())
                .role("ROLE_" + user.getRole().name())
                .userId(user.getId())
                .build();
    }
}
