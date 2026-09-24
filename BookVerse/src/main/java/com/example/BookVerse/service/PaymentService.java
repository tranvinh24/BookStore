package com.example.BookVerse.service;

import com.example.BookVerse.config.VNPayConfig;
import com.example.BookVerse.dto.response.PaymentResponse;
import com.example.BookVerse.dto.response.VNPayCallbackResponse;
import com.example.BookVerse.dto.response.VNPayPaymentUrlResponse;
import com.example.BookVerse.entity.Order;
import com.example.BookVerse.entity.Payment;
import com.example.BookVerse.enums.OrderStatus;
import com.example.BookVerse.enums.PaymentStatus;
import com.example.BookVerse.exception.AppException;
import com.example.BookVerse.exception.ErrorCode;
import com.example.BookVerse.repository.OrderRepository;
import com.example.BookVerse.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository   paymentRepository;
    private final OrderRepository     orderRepository;
    private final VNPayConfig         vnPayConfig;
    private final NotificationService notificationService;

    /**
     * Tạo URL thanh toán VNPay Sandbox có kèm chữ ký số HMAC-SHA512.
     */
    public VNPayPaymentUrlResponse createVNPayPaymentUrl(String userId, String orderId, HttpServletRequest request) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PROCESSED,
                    "Đơn hàng không ở trạng thái chờ thanh toán. Hiện tại: " + order.getStatus());
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PROCESSED, "Đơn hàng đã được thanh toán thành công trước đó.");
        }

        long amount = order.getTotalAmount() * 100; // VNPay quy định nhân 100 với tiền VND

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", order.getId());
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + order.getId());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", VNPayConfig.getIpAddress(request));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        vnpParams.put("vnp_CreateDate", now.format(formatter));
        vnpParams.put("vnp_ExpireDate", now.plusMinutes(15).format(formatter));

        // Sắp xếp các tham số theo thứ tự alphabet
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                
                // Build query string
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String vnpSecureHash = VNPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        query.append("&vnp_SecureHash=").append(vnpSecureHash);
        String paymentUrl = vnPayConfig.getPayUrl() + "?" + query;

        log.info("Da tao VNPay payment URL cho orderId={}, totalAmount={}", orderId, order.getTotalAmount());

        return VNPayPaymentUrlResponse.builder()
                .orderId(order.getId())
                .amount(order.getTotalAmount())
                .paymentUrl(paymentUrl)
                .build();
    }

    /**
     * Xử lý callback / return URL từ VNPay.
     * Kiểm tra chữ ký bảo mật, xác thực kết quả giao dịch và cập nhật database.
     */
    @Transactional
    public VNPayCallbackResponse processVNPayCallback(Map<String, String> queryParams) {
        String vnpSecureHash = queryParams.get("vnp_SecureHash");
        if (vnpSecureHash == null || vnpSecureHash.isBlank()) {
            return VNPayCallbackResponse.builder()
                    .code("99")
                    .message("Thiếu mã kiểm tra tính toàn vẹn chữ ký (vnp_SecureHash)")
                    .paymentStatus(PaymentStatus.FAILED)
                    .build();
        }

        // Tạo bản copy không chứa các trường hash để kiểm tra chữ ký
        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (!"vnp_SecureHash".equals(entry.getKey()) && !"vnp_SecureHashType".equals(entry.getKey())) {
                fields.put(entry.getKey(), entry.getValue());
            }
        }

        String calculatedHash = VNPayConfig.hashAllFields(fields, vnPayConfig.getHashSecret());
        if (!calculatedHash.equalsIgnoreCase(vnpSecureHash)) {
            log.warn("VNPay callback chu ky khong hop le! Calculated={}, Provided={}", calculatedHash, vnpSecureHash);
            return VNPayCallbackResponse.builder()
                    .code("97")
                    .message("Chữ ký bảo mật không hợp lệ (Checksum failed)")
                    .paymentStatus(PaymentStatus.FAILED)
                    .build();
        }

        String orderId       = queryParams.get("vnp_TxnRef");
        String responseCode  = queryParams.get("vnp_ResponseCode");
        String transactionNo = queryParams.get("vnp_TransactionNo");
        String bankCode      = queryParams.get("vnp_BankCode");
        String rawAmount     = queryParams.get("vnp_Amount");
        Long amount = (rawAmount != null && !rawAmount.isBlank()) ? Long.parseLong(rawAmount) / 100 : 0L;

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return VNPayCallbackResponse.builder()
                    .code("01")
                    .message("Không tìm thấy đơn hàng: " + orderId)
                    .orderId(orderId)
                    .paymentStatus(PaymentStatus.FAILED)
                    .build();
        }

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment == null) {
            return VNPayCallbackResponse.builder()
                    .code("02")
                    .message("Không tìm thấy thông tin thanh toán cho đơn hàng: " + orderId)
                    .orderId(orderId)
                    .paymentStatus(PaymentStatus.FAILED)
                    .build();
        }

        // Response code "00" đại diện cho giao dịch thành công trên VNPay
        if ("00".equals(responseCode)) {
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);

            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId("VNPAY-" + (transactionNo != null ? transactionNo : orderId));
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Gửi thông báo thanh toán thành công
            notificationService.notifyOrderStatusChange(order.getUser(), order.getId(), OrderStatus.PAID, null);

            log.info("VNPay thanh toan thanh cong! orderId={}, txnNo={}, amount={}", orderId, transactionNo, amount);

            return VNPayCallbackResponse.builder()
                    .code("00")
                    .message("Giao dịch thanh toán VNPay thành công!")
                    .orderId(orderId)
                    .transactionId(payment.getTransactionId())
                    .bankCode(bankCode)
                    .amount(amount)
                    .paymentStatus(PaymentStatus.SUCCESS)
                    .paidAt(payment.getPaidAt())
                    .build();
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            log.warn("VNPay thanh toan that bai! orderId={}, responseCode={}", orderId, responseCode);

            return VNPayCallbackResponse.builder()
                    .code(responseCode)
                    .message("Giao dịch không thành công hoặc người dùng đã hủy (Mã lỗi: " + responseCode + ")")
                    .orderId(orderId)
                    .bankCode(bankCode)
                    .amount(amount)
                    .paymentStatus(PaymentStatus.FAILED)
                    .build();
        }
    }

    /**
     * Giả lập thanh toán trực tiếp (giữ lại hỗ trợ test nhanh).
     */
    @Transactional
    public PaymentResponse processPayment(String userId, String orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PROCESSED,
                    "Đơn hàng đã được xử lý. Trạng thái hiện tại: " + order.getStatus());
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PROCESSED, "Đơn hàng đã được thanh toán");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId("SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        // Gửi thông báo thanh toán thành công
        notificationService.notifyOrderStatusChange(order.getUser(), order.getId(), OrderStatus.PAID, null);

        log.info("Thanh toan gia lap thanh cong. orderId={}, transactionId={}", orderId, payment.getTransactionId());

        return toResponse(payment);
    }

    public PaymentResponse getPaymentByOrder(String userId, String orderId) {
        orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
