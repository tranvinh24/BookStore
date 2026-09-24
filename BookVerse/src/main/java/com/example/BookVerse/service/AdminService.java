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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
@Transactional(readOnly = true)
public class AdminService {

    private final OrderRepository     orderRepository;
    private final PaymentRepository   paymentRepository;
    private final UserRepository      userRepository;
    private final BookRepository      bookRepository;
    private final BookMapper          bookMapper;
    private final NotificationService notificationService;

    // =========================================================
    // QUAN LY DON HANG
    // =========================================================

    /**
     * Lay danh sach tat ca don hang voi phan trang.
     * Dung JOIN FETCH de load user + payment trong 1 query, tranh N+1.
     */
    public PageResponse<OrderSummaryResponse> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderRepository.findAllWithUserAndPayment(pageable);
        return PageResponse.of(orders, this::toOrderSummary);
    }

    /**
     * Lay danh sach don hang theo trang thai voi phan trang.
     * Dung JOIN FETCH de load user + payment trong 1 query, tranh N+1.
     */
    public PageResponse<OrderSummaryResponse> getOrdersByStatus(OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderRepository.findByStatusWithUserAndPayment(status, pageable);
        return PageResponse.of(orders, this::toOrderSummary);
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
     * Khi CANCELLED: tu dong hoan tra so luong ton kho cho tung sach.
     * Khi DELIVERED hoac PAID: tu dong xac nhan payment = SUCCESS va cap nhat paidAt (dam bao doanh thu duoc tinh).
     */
    @Transactional
    public OrderSummaryResponse updateOrderStatus(String orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        validateStatusTransition(order, request.getStatus());

        // Hoàn trả tồn kho nếu đơn bị hủy
        if (request.getStatus() == OrderStatus.CANCELLED) {
            if (order.getItems() != null) {
                for (com.example.BookVerse.entity.OrderItem item : order.getItems()) {
                    Book book = bookRepository.findById(item.getBook().getId()).orElse(null);
                    if (book != null) {
                        int restored = (book.getStock() == null ? 0 : book.getStock()) + item.getQuantity();
                        book.setStock(restored);
                        bookRepository.save(book);
                        log.info("Hoan tra ton kho: bookId={} +{} (tong={})", book.getId(), item.getQuantity(), restored);
                    }
                }
            }
            paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
            });
        }

        // Khi giao hàng thành công (DELIVERED) hoặc admin xác nhận đã thanh toán (PAID):
        // Đối với đơn COD, khi giao hàng thành công mới chính thức thu tiền -> Payment = SUCCESS, paidAt = now()
        if (request.getStatus() == OrderStatus.DELIVERED || request.getStatus() == OrderStatus.PAID) {
            paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
                if (payment.getStatus() != PaymentStatus.SUCCESS) {
                    payment.setStatus(PaymentStatus.SUCCESS);
                    if (payment.getPaidAt() == null) {
                        payment.setPaidAt(LocalDateTime.now());
                    }
                    paymentRepository.save(payment);
                    log.info("Xac nhan thanh toan khi cap nhat don {}: orderId={}, method={}", request.getStatus(), orderId, payment.getMethod());
                }
            });
        }

        order.setStatus(request.getStatus());
        if (request.getTrackingNote() != null && !request.getTrackingNote().isBlank()) {
            order.setTrackingNote(request.getTrackingNote());
        }

        order = orderRepository.save(order);
        log.info("Admin cap nhat don hang {} -> {}", orderId, request.getStatus());

        // Tạo thông báo cho khách hàng
        notificationService.notifyOrderStatusChange(order.getUser(), order.getId(), request.getStatus(), request.getTrackingNote());

        return toOrderSummary(order);
    }

    /**
     * Kiem tra thu tu chuyen trang thai hop le theo tung phuong thuc thanh toan:
     * - COD: PENDING -> PROCESSING (Chuan bi) -> SHIPPING (Giao) -> DELIVERED (Giao thanh cong & Thu tien)
     * - VNPAY/Online: PENDING -> PAID (Da tra) -> PROCESSING -> SHIPPING -> DELIVERED
     */
    private void validateStatusTransition(Order order, OrderStatus next) {
        OrderStatus current = order.getStatus();
        if (current == next) return;

        if (next == OrderStatus.CANCELLED) {
            if (current == OrderStatus.DELIVERED) {
                throw new AppException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION,
                        "Không thể hủy đơn hàng đã giao thành công.");
            }
            return;
        }

        Payment payment = order.getPayment();
        if (payment == null) {
            payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        }
        boolean isCOD = payment != null && payment.getMethod() == com.example.BookVerse.enums.PaymentMethod.COD;

        boolean valid = switch (current) {
            case PENDING -> isCOD 
                    ? (next == OrderStatus.PROCESSING || next == OrderStatus.PAID)
                    : (next == OrderStatus.PAID || next == OrderStatus.PROCESSING);
            case PAID -> next == OrderStatus.PROCESSING;
            case PROCESSING -> next == OrderStatus.SHIPPING;
            case SHIPPING -> next == OrderStatus.DELIVERED;
            default -> false;
        };

        if (!valid) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION,
                    "Không thể chuyển trạng thái từ " + current + " sang " + next);
        }
    }

    // =========================================================
    // QUAN LY NGUOI DUNG
    // =========================================================

    /**
     * Lay danh sach tat ca user voi phan trang.
     */
    public PageResponse<UserResponse> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = userRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.of(users, this::toUserResponse);
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
     * Dung query DB thay vi findAll() + filter Java.
     */
    public List<BookRespone> getLowStockBooks(int threshold) {
        Pageable pageable = PageRequest.of(0, 100); // gioi han 100 sach hien thi
        return bookRepository.findLowStockBooks(threshold, pageable)
                .getContent()
                .stream()
                .map(bookMapper::toBookRespone)
                .toList();
    }

    // =========================================================
    // HELPERS
    // =========================================================

    /**
     * Chuyen Order sang OrderSummaryResponse.
     * Payment da duoc JOIN FETCH cung order — khong goi DB rieng.
     */
    private OrderSummaryResponse toOrderSummary(Order order) {
        Payment payment = order.getPayment();
        if (payment == null) {
            payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        }
        String userName = "Ẩn danh";
        String userId = null;
        if (order.getUser() != null) {
            userId = order.getUser().getId();
            userName = order.getUser().getFullName() != null && !order.getUser().getFullName().isBlank()
                    ? order.getUser().getFullName()
                    : order.getUser().getUsername();
        }
        return OrderSummaryResponse.builder()
                .id(order.getId())
                .userId(userId)
                .userName(userName)
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .trackingNote(order.getTrackingNote())
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .paymentMethod(payment != null && payment.getMethod() != null ? payment.getMethod().name() : null)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .itemCount(order.getItems() != null ? order.getItems().size() : 0)
                .build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .userName(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .sdt(user.getSdt())
                .role(user.getRole())
                .avatarPath(user.getAvatarPath())
                .dateOfBirth(user.getDateOfBirth())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}