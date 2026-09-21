package com.example.BookVerse.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    @Size(max = 150, message = "Tên hiển thị tối đa 150 ký tự")
    private String fullName;

    @Email(message = "Email không đúng định dạng")
    private String email;

    @Size(max = 15, message = "Số điện thoại tối đa 15 ký tự")
    private String sdt;

    private LocalDate dateOfBirth;
}