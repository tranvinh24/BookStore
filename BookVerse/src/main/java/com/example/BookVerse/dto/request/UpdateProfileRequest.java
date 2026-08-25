package com.example.BookVerse.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Email(message = "Email khong dung dinh dang")
    private String email;

    @Size(max = 15, message = "So dien thoai toi da 15 ky tu")
    private String sdt;

    @Size(min = 6, message = "Mat khau toi thieu 6 ky tu")
    private String newPassword;

    /** Mat khau hien tai - bat buoc khi doi mat khau */
    private String currentPassword;
}