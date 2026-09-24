package com.example.BookVerse.service;

import com.example.BookVerse.dto.request.ChangePasswordRequest;
import com.example.BookVerse.dto.request.UpdateProfileRequest;
import com.example.BookVerse.dto.response.UserResponse;
import com.example.BookVerse.entity.User;
import com.example.BookVerse.exception.AppException;
import com.example.BookVerse.exception.ErrorCode;
import com.example.BookVerse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * ProfileService xử lý việc xem và cập nhật thông tin cá nhân user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final CloudinaryService cloudinaryService;

    private static final List<String> ALLOWED_IMAGE_TYPES =
            List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    /** Lấy thông tin cá nhân user hiện tại */
    @Cacheable(value = "profiles", key = "#userId")
    public UserResponse getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return toUserResponse(user);
    }

    /** Cập nhật thông tin hồ sơ cá nhân (fullName, email, sdt, dateOfBirth) */
    @Caching(evict = {
            @CacheEvict(value = "profiles",    key = "#userId"),
            @CacheEvict(value = "userDetails", key = "#result.userName")
    })
    @Transactional
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!request.getEmail().equals(user.getEmail())
                    && userRepository.existsByEmail(request.getEmail())) {
                throw new AppException(ErrorCode.EMAIL_EXISTED);
            }
            user.setEmail(request.getEmail());
        }

        if (request.getSdt() != null) {
            user.setSdt(request.getSdt());
        }

        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }

        return toUserResponse(userRepository.save(user));
    }

    /** Đổi mật khẩu — yêu cầu nhập đúng mật khẩu hiện tại */
    @Caching(evict = {
            @CacheEvict(value = "profiles",    key = "#userId"),
            @CacheEvict(value = "userDetails", allEntries = true)
    })
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("User {} đổi mật khẩu thành công", userId);
    }

    /** Upload ảnh đại diện lên Cloudinary */
    @CacheEvict(value = "profiles", key = "#userId")
    @Transactional
    public UserResponse uploadAvatar(String userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
        }

        BufferedImage original = ImageIO.read(file.getInputStream());
        if (original == null) {
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
        }

        // Upload lên Cloudinary — trả về URL công khai
        String avatarUrl = cloudinaryService.uploadAvatar(original, userId);
        user.setAvatarPath(avatarUrl);
        userRepository.save(user);

        log.info("User {} cập nhật avatar thành công: {}", userId, avatarUrl);
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .userName(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .sdt(user.getSdt())
                .dateOfBirth(user.getDateOfBirth())
                .avatarPath(user.getAvatarPath())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

}