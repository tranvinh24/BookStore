package com.example.BookVerse.controller;

import com.example.BookVerse.dto.response.NotificationResponse;
import com.example.BookVerse.entity.User;
import com.example.BookVerse.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API quản lý thông báo người dùng.
 * Yêu cầu đăng nhập.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * GET /api/notifications
     * Lấy danh sách thông báo của user hiện tại.
     */
    @GetMapping
    public List<NotificationResponse> getNotifications(@AuthenticationPrincipal User user) {
        return notificationService.getUserNotifications(user.getId());
    }

    /**
     * GET /api/notifications/unread-count
     * Lấy số lượng thông báo chưa đọc.
     */
    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(@AuthenticationPrincipal User user) {
        long count = notificationService.getUnreadCount(user.getId());
        return Map.of("unreadCount", count);
    }

    /**
     * PUT /api/notifications/{id}/read
     * Đánh dấu 1 thông báo là đã đọc.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        notificationService.markAsRead(user.getId(), id);
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/notifications/read-all
     * Đánh dấu tất cả thông báo là đã đọc.
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok().build();
    }
}
