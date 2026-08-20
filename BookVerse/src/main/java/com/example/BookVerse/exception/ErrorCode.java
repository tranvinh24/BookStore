package com.example.BookVerse.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Định nghĩa mã lỗi chuẩn toàn hệ thống BookVerse.
 */
@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION("UNCATEGORIZED", "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY("INVALID_KEY", "Khóa không hợp lệ", HttpStatus.BAD_REQUEST),
    USER_EXISTED("USER_EXISTED", "Tên đăng nhập đã tồn tại", HttpStatus.CONFLICT),
    EMAIL_EXISTED("EMAIL_EXISTED", "Email đã được sử dụng", HttpStatus.CONFLICT),
    USER_NOT_EXISTED("USER_NOT_EXISTED", "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED("UNAUTHENTICATED", "Chưa xác thực hoặc token không hợp lệ", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("UNAUTHORIZED", "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),
    BOOK_NOT_FOUND("BOOK_NOT_FOUND", "Không tìm thấy sách", HttpStatus.NOT_FOUND),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không chính xác", HttpStatus.UNAUTHORIZED),
    VALIDATION_ERROR("VALIDATION_ERROR", "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    FILE_STORAGE_ERROR("FILE_STORAGE_ERROR", "Lỗi lưu trữ tập tin", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_NOT_FOUND("FILE_NOT_FOUND", "Không tìm thấy tập tin ảnh", HttpStatus.NOT_FOUND),
    INVALID_FILE_FORMAT("INVALID_FILE_FORMAT", "Định dạng tập tin không được hỗ trợ", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE("FILE_TOO_LARGE", "Dung lượng tập tin vượt quá giới hạn", HttpStatus.PAYLOAD_TOO_LARGE),
    INSUFFICIENT_STOCK("INSUFFICIENT_STOCK", "Số lượng sách trong kho không đủ", HttpStatus.BAD_REQUEST),

    // Wishlist
    WISHLIST_ALREADY_EXISTS("WISHLIST_ALREADY_EXISTS", "Sách đã có trong danh sách yêu thích", HttpStatus.CONFLICT),
    WISHLIST_NOT_FOUND("WISHLIST_NOT_FOUND", "Không tìm thấy sách trong danh sách yêu thích", HttpStatus.NOT_FOUND),

    // Cart
    CART_NOT_FOUND("CART_NOT_FOUND", "Giỏ hàng không tồn tại", HttpStatus.NOT_FOUND),
    CART_ITEM_NOT_FOUND("CART_ITEM_NOT_FOUND", "Không tìm thấy sản phẩm trong giỏ hàng", HttpStatus.NOT_FOUND),
    CART_EMPTY("CART_EMPTY", "Giỏ hàng đang trống, không thể đặt hàng", HttpStatus.BAD_REQUEST),

    // Order
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "Không tìm thấy đơn hàng", HttpStatus.NOT_FOUND),

    // Payment
    PAYMENT_NOT_FOUND("PAYMENT_NOT_FOUND", "Không tìm thấy thông tin thanh toán", HttpStatus.NOT_FOUND),
    PAYMENT_ALREADY_PROCESSED("PAYMENT_ALREADY_PROCESSED", "Đơn hàng đã được thanh toán", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
