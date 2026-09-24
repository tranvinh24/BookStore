package com.example.BookVerse.dto.response;

import com.example.BookVerse.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private String id;
    private String title;
    private String message;
    private String orderId;
    private NotificationType type;
    private boolean isRead;
    private LocalDateTime createdAt;
    private String timeAgo;
}
