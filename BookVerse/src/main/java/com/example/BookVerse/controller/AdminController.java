package com.example.BookVerse.controller;

import com.example.BookVerse.dto.request.AdminUpdateUserRequest;
import com.example.BookVerse.dto.request.UpdateOrderStatusRequest;
import com.example.BookVerse.dto.request.UpdateStockRequest;
import com.example.BookVerse.dto.response.BookRespone;
import com.example.BookVerse.dto.response.OrderSummaryResponse;
import com.example.BookVerse.dto.response.PageResponse;
import com.example.BookVerse.dto.response.RevenueResponse;
import com.example.BookVerse.dto.response.UserResponse;
import com.example.BookVerse.enums.OrderStatus;
import com.example.BookVerse.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API quan tri he thong — chi danh cho ROLE_ADMIN.
 * Phan quyen duoc cap hinh trong SecurityConfig.
 *
 * Order management : /api/admin/orders/**
 * User management  : /api/admin/users/**
 * Revenue stats    : /api/admin/revenue/**
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // =========================================================
    // ORDER MANAGEMENT
    // =========================================================

    /**
     * GET /api/admin/orders?page=0&size=20&status=PENDING
     * Lay danh sach don hang co phan trang va loc theo trang thai.
     */
    @GetMapping("/orders")
    public PageResponse<OrderSummaryResponse> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        if (status != null) {
            return adminService.getOrdersByStatus(status, page, size);
        }
        return adminService.getAllOrders(page, size);
    }

    /**
     * GET /api/admin/orders/{id}
     * Xem chi tiet 1 don hang.
     */
    @GetMapping("/orders/{id}")
    public OrderSummaryResponse getOrderById(@PathVariable String id) {
        return adminService.getOrderById(id);
    }

    /**
     * PUT /api/admin/orders/{id}/status
     * Cap nhat trang thai don hang + tracking note.
     * Thu tu: PENDING -> PAID -> PROCESSING -> SHIPPING -> DELIVERED (hoac CANCELLED)
     */
    @PutMapping("/orders/{id}/status")
    public OrderSummaryResponse updateOrderStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return adminService.updateOrderStatus(id, request);
    }

    // =========================================================
    // USER MANAGEMENT
    // =========================================================

    /**
     * GET /api/admin/users?page=0&size=20
     * Lay danh sach tat ca nguoi dung voi phan trang.
     */
    @GetMapping("/users")
    public PageResponse<UserResponse> getAllUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.getAllUsers(page, size);
    }

    /**
     * GET /api/admin/users/{id}
     * Xem thong tin 1 user.
     */
    @GetMapping("/users/{id}")
    public UserResponse getUserById(@PathVariable String id) {
        return adminService.getUserById(id);
    }

    /**
     * PUT /api/admin/users/{id}
     * Cap nhat email, sdt, role cua user.
     */
    @PutMapping("/users/{id}")
    public UserResponse updateUser(
            @PathVariable String id,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        return adminService.updateUser(id, request);
    }

    /**
     * DELETE /api/admin/users/{id}
     * Xoa tai khoan user.
     */
    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable String id) {
        adminService.deleteUser(id);
    }

    // =========================================================
    // REVENUE STATISTICS
    // =========================================================

    /**
     * GET /api/admin/revenue/monthly?month=2026-08
     * Thong ke doanh thu theo thang.
     * Neu khong truyen month -> dung thang hien tai.
     */
    @GetMapping("/revenue/monthly")
    public RevenueResponse getMonthlyRevenue(
            @RequestParam(required = false) String month) {
        return adminService.getMonthlyRevenue(month);
    }

    /**
     * GET /api/admin/revenue/daily?date=2026-08-25
     * Thong ke doanh thu theo ngay.
     * Neu khong truyen date -> dung ngay hom nay.
     */
    @GetMapping("/revenue/daily")
    public RevenueResponse getDailyRevenue(
            @RequestParam(required = false) String date) {
        return adminService.getDailyRevenue(date);
    }

    // =========================================================
    // INVENTORY MANAGEMENT
    // =========================================================

    /**
     * PATCH /api/admin/books/{bookId}/stock
     * Cap nhat so luong ton kho cua 1 sach.
     */
    @PatchMapping("/books/{bookId}/stock")
    public BookRespone updateStock(
            @PathVariable String bookId,
            @Valid @RequestBody UpdateStockRequest request) {
        return adminService.updateStock(bookId, request);
    }

    /**
     * GET /api/admin/books/low-stock?threshold=5
     * Lay danh sach sach co ton kho thap.
     */
    @GetMapping("/books/low-stock")
    public List<BookRespone> getLowStockBooks(
            @RequestParam(defaultValue = "5") int threshold) {
        return adminService.getLowStockBooks(threshold);
    }
}