package com.example.BookVerse.service;

import com.example.BookVerse.dto.response.NotificationResponse;
import com.example.BookVerse.entity.Notification;
import com.example.BookVerse.entity.User;
import com.example.BookVerse.enums.NotificationType;
import com.example.BookVerse.enums.OrderStatus;
import com.example.BookVerse.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Tự động sinh thông báo cho người dùng khi đơn hàng có thay đổi trạng thái.
     */
    @Transactional
    public void notifyOrderStatusChange(User user, String orderId, OrderStatus status, String trackingNote) {
        if (user == null || orderId == null) return;

        String shortId = orderId.length() > 8 ? orderId.substring(0, 8) : orderId;
        String title;
        String message;

        switch (status) {
            case PENDING -> {
                title = "Đơn hàng mới đã tạo";
                message = "Đơn hàng #" + shortId + " của bạn đã được tiếp nhận và đang chờ xử lý.";
            }
            case PAID -> {
                title = "Thanh toán thành công";
                message = "Đơn hàng #" + shortId + " đã được thanh toán thành công.";
            }
            case PROCESSING -> {
                title = "Đơn hàng đang chuẩn bị";
                message = "Đơn hàng #" + shortId + " đang được đóng gói và chuẩn bị bàn giao vận chuyển.";
            }
            case SHIPPING -> {
                title = "Đơn hàng đang giao";
                message = "Đơn hàng #" + shortId + " đang được giao đến bạn."
                        + (trackingNote != null && !trackingNote.isBlank() ? " Ghi chú vận đơn: " + trackingNote : "");
            }
            case DELIVERED -> {
                title = "Đơn hàng giao thành công";
                message = "Đơn hàng #" + shortId + " đã giao thành công. Chúc bạn có trải nghiệm đọc sách tuyệt vời!";
            }
            case CANCELLED -> {
                title = "Đơn hàng đã hủy";
                message = "Đơn hàng #" + shortId + " của bạn đã bị hủy.";
            }
            default -> {
                title = "Cập nhật đơn hàng";
                message = "Đơn hàng #" + shortId + " vừa được cập nhật trạng thái: " + status;
            }
        }

        Notification notification = Notification.builder()
                .user(user)
                .orderId(orderId)
                .title(title)
                .message(message)
                .type(NotificationType.ORDER)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("Da tao thong bao orderStatusChange cho userId={}, orderId={}, status={}", user.getId(), orderId, status);
    }

    /**
     * Lấy danh sách 30 thông báo gần nhất của người dùng.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(String userId) {
        return notificationRepository.findTop30ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Lấy số lượng thông báo chưa đọc của người dùng.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Đánh dấu 1 thông báo là đã đọc.
     */
    @Transactional
    public void markAsRead(String userId, String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    /**
     * Đánh dấu tất cả thông báo của người dùng là đã đọc.
     */
    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .orderId(n.getOrderId())
                .type(n.getType())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .timeAgo(formatTimeAgo(n.getCreatedAt()))
                .build();
    }

    private String formatTimeAgo(LocalDateTime dt) {
        if (dt == null) return "";
        Duration duration = Duration.between(dt, LocalDateTime.now());
        long seconds = duration.getSeconds();

        if (seconds < 60) return "Vừa xong";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " phút trước";
        long hours = minutes / 60;
        if (hours < 24) return hours + " giờ trước";
        long days = hours / 24;
        if (days < 30) return days + " ngày trước";
        long months = days / 30;
        if (months < 12) return months + " tháng trước";
        return (months / 12) + " năm trước";
    }
}
