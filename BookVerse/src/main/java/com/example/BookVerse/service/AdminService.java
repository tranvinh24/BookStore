package com.example.BookVerse.service;

import com.example.BookVerse.dto.request.AdminUpdateUserRequest;
import com.example.BookVerse.dto.request.UpdateOrderStatusRequest;
import com.example.BookVerse.dto.request.UpdateStockRequest;
import com.example.BookVerse.dto.response.*;
import com.example.BookVerse.dto.response.BookRespone;
import com.example.BookVerse.entity.Book;
import com.example.BookVerse.entity.Order;
import com.example.BookVerse.entity.Payment;
import com.example.BookVerse.entity.User;
import com.example.BookVerse.enums.OrderStatus;
import com.example.BookVerse.enums.PaymentStatus;
import com.example.BookVerse.exception.AppException;
import com.example.BookVerse.exception.ErrorCode;
import com.example.BookVerse.repository.BookRepository;
import com.example.BookVerse.repository.OrderRepository;
import com.example.BookVerse.repository.PaymentRepository;
import com.example.BookVerse.repository.UserRepository;
import com.example.BookVerse.Mapper.BookMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * AdminService xu ly cac nghiep vu quan tri:
 * - Quan ly don hang (xem, cap nhat trang thai, tracking)
 * - Quan ly tai khoan user
 * - Thong ke doanh thu theo ngay / thang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final OrderRepository   orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository    userRepository;
    private final BookRepository    bookRepository;
    private final BookMapper        bookMapper;

    // =========================================================
    // QUAN LY DON HANG
    // =========================================================

    /** Lay tat ca don hang, sap xep moi nhat truoc */
    public List<OrderSummaryResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toOrderSummary)
                .toList();
    }

    /** Lay danh sach don hang theo trang thai */
    public List<OrderSummaryResponse> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(this::toOrderSummary)
                .toList();
    }

    /** Lay chi tiet 1 don hang (admin co the xem bat ky don nao) */
    public OrderSummaryResponse getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return toOrderSummary(order);
    }

    /**
     * Cap nhat trang thai don hang + tracking note.
     * Thu tu hop le: PENDING -> PAID -> PROCESSING -> SHIPPING -> DELIVERED
     * Admin co the CANCEL don hang o trang thai PENDING hoac PAID.
     */
    @Transactional
    public OrderSummaryResponse updateOrderStatus(String orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        validateStatusTransition(order.getStatus(), request.getStatus());

        order.setStatus(request.getStatus());
        if (request.getTrackingNote() != null && !request.getTrackingNote().isBlank()) {
            order.setTrackingNote(request.getTrackingNote());
        }

        order = orderRepository.save(order);
        log.info("Admin cap nhat don hang {} -> {}", orderId, request.getStatus());
        return toOrderSummary(order);
    }

    /** Kiem tra thu tu chuyen trang thai hop le */
    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        if (next == OrderStatus.CANCELLED) {
            if (current != OrderStatus.PENDING && current != OrderStatus.PAID) {
                throw new AppException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION,
                        "Chi co the huy don o trang thai PENDING hoac PAID. Hien tai: " + current);
            }
            return;
        }
        boolean valid = switch (current) {
            case PENDING    -> next == OrderStatus.PAID;
            case PAID       -> next == OrderStatus.PROCESSING;
            case PROCESSING -> next == OrderStatus.SHIPPING;
            case SHIPPING   -> next == OrderStatus.DELIVERED;
            default -> false;
        };
        if (!valid) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION,
                    "Khong the chuyen trang thai tu " + current + " sang " + next);
        }
    }

    // =========================================================
    // QUAN LY NGUOI DUNG
    // =========================================================

    /** Lay danh sach tat ca user */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    /** Lay thong tin 1 user theo ID */
    public UserResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return toUserResponse(user);
    }

    /** Cap nhat thong tin user (email, sdt, role) */
    @Transactional
    public UserResponse updateUser(String userId, AdminUpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            // Kiem tra email khong bi trung voi user khac
            if (!request.getEmail().equals(user.getEmail())
                    && userRepository.existsByEmail(request.getEmail())) {
                throw new AppException(ErrorCode.EMAIL_EXISTED);
            }
            user.setEmail(request.getEmail());
        }
        if (request.getSdt() != null) {
            user.setSdt(request.getSdt());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        return toUserResponse(userRepository.save(user));
    }

    /** Xoa tai khoan user */
    @Transactional
    public void deleteUser(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }
        userRepository.deleteById(userId);
        log.info("Admin da xoa user {}", userId);
    }

    // =========================================================
    // THONG KE DOANH THU
    // =========================================================

    /**
     * Thong ke doanh thu theo thang (format: yyyy-MM).
     * Neu month = null -> dung thang hien tai.
     */
    public RevenueResponse getMonthlyRevenue(String month) {
        YearMonth ym = (month != null && !month.isBlank())
                ? YearMonth.parse(month)
                : YearMonth.now();

        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to   = ym.atEndOfMonth().atTime(23, 59, 59);

        return buildRevenue(ym.toString(), from, to);
    }

    /**
     * Thong ke doanh thu theo ngay (format: yyyy-MM-dd).
     * Neu date = null -> dung ngay hom nay.
     */
    public RevenueResponse getDailyRevenue(String date) {
        LocalDate day = (date != null && !date.isBlank())
                ? LocalDate.parse(date)
                : LocalDate.now();

        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to   = day.atTime(23, 59, 59);

        return buildRevenue(day.toString(), from, to);
    }

    private RevenueResponse buildRevenue(String period, LocalDateTime from, LocalDateTime to) {
        Long revenue = paymentRepository.sumAmountByStatusAndPaidAtBetween(PaymentStatus.SUCCESS, from, to);
        Long paid    = paymentRepository.countByStatusAndPaidAtBetween(PaymentStatus.SUCCESS, from, to);
        long total   = orderRepository.countAllOrders();
        long pending = orderRepository.countByStatus(OrderStatus.PENDING);

        return RevenueResponse.builder()
                .period(period)
                .totalRevenue(revenue == null ? 0L : revenue)
                .totalOrders(total)
                .paidOrders(paid == null ? 0L : paid)
                .pendingOrders(pending)
                .build();
    }

    // =========================================================
    // QUAN LY TON KHO
    // =========================================================

    /**
     * Cap nhat so luong ton kho cua 1 sach.
     * Chi cap nhat rieng stock ma khong can thay doi cac truong khac.
     */
    @Transactional
    public BookRespone updateStock(String bookId, UpdateStockRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));
        book.setStock(request.getStock());
        book = bookRepository.save(book);
        log.info("Admin cap nhat ton kho sach id={} -> stock={}", bookId, request.getStock());
        return bookMapper.toBookRespone(book);
    }

    /**
     * Lay danh sach sach co ton kho thap (stock <= threshold).
     * Mac dinh threshold = 5.
     */
    public java.util.List<BookRespone> getLowStockBooks(int threshold) {
        return bookRepository.findAll().stream()
                .filter(b -> b.getStock() != null && b.getStock() <= threshold)
                .map(bookMapper::toBookRespone)
                .sorted(java.util.Comparator.comparing(BookRespone::getStock))
                .toList();
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private OrderSummaryResponse toOrderSummary(Order order) {
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        return OrderSummaryResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .userName(order.getUser().getUsername())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .trackingNote(order.getTrackingNote())
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .paymentMethod(payment != null ? payment.getMethod().name() : null)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .itemCount(order.getItems() != null ? order.getItems().size() : 0)
                .build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .userName(user.getUsername())
                .email(user.getEmail())
                .sdt(user.getSdt())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}