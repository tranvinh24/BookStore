package com.example.BookVerse.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Cấu trúc response lỗi chuẩn trả về cho client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String code;
    private String message;
    private String path;
    private Map<String, String> validationErrors;
}
