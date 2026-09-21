package com.example.BookVerse.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response trả về sau khi đăng nhập / đăng ký thành công.
 * Frontend lưu token vào localStorage để gửi kèm các request tiếp theo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthResponse {

    /** JWT Bearer token */
    String token;

    /** Tên đăng nhập */
    String userName;

    /** Họ và tên hiển thị */
    String fullName;

    /** Đường dẫn ảnh đại diện */
    String avatarPath;

    /** Role: ROLE_USER hoặc ROLE_ADMIN */
    String role;

    /** ID người dùng — tiện lợi cho frontend */
    String userId;
}
