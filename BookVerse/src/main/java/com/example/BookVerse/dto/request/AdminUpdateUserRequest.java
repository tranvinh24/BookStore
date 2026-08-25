package com.example.BookVerse.dto.request;

import com.example.BookVerse.enums.Role;
import lombok.Data;

@Data
public class AdminUpdateUserRequest {
    private String email;
    private String sdt;
    private Role role;
}