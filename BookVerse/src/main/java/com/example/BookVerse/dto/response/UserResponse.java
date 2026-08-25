package com.example.BookVerse.dto.response;

import com.example.BookVerse.enums.Role;
import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String id;
    private String userName;
    private String email;
    private String sdt;
    private Role role;
    private LocalDate createdAt;
    private LocalDate updatedAt;
}