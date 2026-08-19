package com.example.BookVerse.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request body cho POST /api/auth/login
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    String userName;

    @NotBlank(message = "Mật khẩu không được để trống")
    String password;
}
