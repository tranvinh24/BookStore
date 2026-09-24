package com.example.BookVerse.repository;

import com.example.BookVerse.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository cho User entity.
 * Cung cấp các query cần thiết cho AuthService và UserDetailsService.
 */
public interface UserRepository extends JpaRepository<User, String> {

    /** Tìm user theo userName — dùng cho đăng nhập và loadUserByUsername */
    Optional<User> findByUserName(String userName);

    /** Kiểm tra userName đã tồn tại — dùng khi đăng ký */
    boolean existsByUserName(String userName);

    /** Kiểm tra email đã tồn tại — dùng khi đăng ký */
    boolean existsByEmail(String email);

    /** Lấy danh sách user có phân trang, mới nhất trước — dùng cho admin */
    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
