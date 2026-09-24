package com.example.BookVerse.service;

import com.example.BookVerse.dto.request.CheckoutRequest;
import com.example.BookVerse.dto.response.OrderItemResponse;
import com.example.BookVerse.dto.response.OrderResponse;
import com.example.BookVerse.dto.response.PaymentResponse;
import com.example.BookVerse.entity.*;
import com.example.BookVerse.enums.OrderStatus;
import com.example.BookVerse.enums.PaymentStatus;
import com.example.BookVerse.exception.AppException;
import com.example.BookVerse.exception.ErrorCode;
import com.example.BookVerse.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository     orderRepository;
    private final CartRepository      cartRepository;
    private final BookRepository      bookRepository;
    private final UserRepository      userRepository;
    private final PaymentRepository   paymentRepository;
    private final NotificationService notificationService;

    /**
     * Checkout: Tạo Order từ giỏ hàng.
     * Dùng pessimistic lock khi trừ tồn kho để tránh race condition.
     */
    @Transactional
    public OrderResponse checkout(String userId, CheckoutRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        // Tạo Order
        Order order = Order.builder()
                .user(user)
                .shippingAddress(request.getShippingAddress())
                .status(OrderStatus.PENDING)
                .totalAmount(0L)
                .items(new ArrayList<>())
                .build();

        order = orderRepository.save(order);

        long total = 0L;

        // Tạo OrderItem và trừ tồn kho (dùng pessimistic lock để tránh overselling)
        for (CartItem cartItem : cart.getItems()) {
            Book book = bookRepository.findByIdWithPessimisticLock(cartItem.getBook().getId())
                    .orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));

            if (book.getStock() == null || book.getStock() < cartItem.getQuantity()) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                        "Sach '" + book.getTitle() + "' khong du so luong. Con lai: "
                        + (book.getStock() == null ? 0 : book.getStock()));
            }

            long priceAtOrder = book.getPrice() == null ? 0L : book.getPrice();

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .book(book)
                    .quantity(cartItem.getQuantity())
                    .priceAtOrder(priceAtOrder)
                    .build();

            order.getItems().add(orderItem);
            total += priceAtOrder * cartItem.getQuantity();

            // Trừ tồn kho
            book.setStock(book.getStock() - cartItem.getQuantity());
            bookRepository.save(book);
        }

        order.setTotalAmount(total);
        order = orderRepository.save(order);

        // Tạo Payment với trạng thái PENDING
        Payment payment = Payment.builder()
                .order(order)
                .amount(total)
                .method(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        // Xóa giỏ hàng sau khi đặt thành công
        cart.getItems().clear();
        cartRepository.save(cart);

        // Gửi thông báo đặt hàng thành công cho khách hàng
        notificationService.notifyOrderStatusChange(user, order.getId(), OrderStatus.PENDING, null);

        log.info("Checkout thanh cong. orderId={}, userId={}, total={}", order.getId(), userId, total);

        return toResponse(order, payment);
    }

    public List<OrderResponse> getMyOrders(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(o -> {
                    Payment payment = paymentRepository.findByOrderId(o.getId()).orElse(null);
                    return toResponse(o, payment);
                })
                .toList();
    }

    public OrderResponse getOrderDetail(String userId, String orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        return toResponse(order, payment);
    }

    private OrderResponse toResponse(Order order, Payment payment) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .bookId(item.getBook().getId())
                        .title(item.getBook().getTitle())
                        .author(item.getBook().getAuthor())
                        .quantity(item.getQuantity())
                        .priceAtOrder(item.getPriceAtOrder())
                        .subtotal(item.getPriceAtOrder() * item.getQuantity())
                        .build())
                .toList();

        PaymentResponse paymentResponse = payment != null ? PaymentResponse.builder()
                .id(payment.getId())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build() : null;

        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .trackingNote(order.getTrackingNote())
                .items(items)
                .payment(paymentResponse)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
