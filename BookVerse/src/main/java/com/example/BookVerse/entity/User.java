package com.example.BookVerse.entity;

import com.example.BookVerse.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Entity người dùng — implement UserDetails để tích hợp Spring Security.
 * Ánh xạ theo ERD: bảng users với các cột id, userName, password, role,
 * email, sdt, createdAt, updatedAt.
 */
@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String userName;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, unique = true)
    private String email;

    /** Số điện thoại — tùy chọn */
    private String sdt;

    /** Tên hiển thị (khác với userName dùng để đăng nhập) */
    @Column(length = 150)
    private String fullName;

    /** Ngày sinh */
    private LocalDate dateOfBirth;

    /** Đường dẫn ảnh đại diện (avatarPath) — lưu dưới dạng path tương đối */
    private String avatarPath;

    @Column(updatable = false)
    private LocalDate createdAt;

    private LocalDate updatedAt;

    // ──────────────────────────────────────────────
    // Lifecycle hooks
    // ──────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDate.now();
    }

    // ──────────────────────────────────────────────
    // UserDetails implementation
    // ──────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Spring Security sử dụng getUsername() làm principal.
     * Trả về userName (không phải fullName).
     */
    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public boolean isAccountNonExpired()     { return true; }

    @Override
    public boolean isAccountNonLocked()      { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled()               { return true; }
}
