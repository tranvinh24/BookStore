package com.example.BookVerse.dto.response;

import com.example.BookVerse.enums.Role;
import lombok.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private String id;
    private String userName;
    private String fullName;
    private String email;
    private String sdt;
    private LocalDate dateOfBirth;
    private String avatarPath;
    private Role role;
    private LocalDate createdAt;
    private LocalDate updatedAt;
}