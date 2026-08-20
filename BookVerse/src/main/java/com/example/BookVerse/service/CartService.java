package com.example.BookVerse.service;

import com.example.BookVerse.dto.request.AddToCartRequest;
import com.example.BookVerse.dto.request.UpdateCartItemRequest;
import com.example.BookVerse.dto.response.CartItemResponse;
import com.example.BookVerse.dto.response.CartResponse;
import com.example.BookVerse.entity.Book;
import com.example.BookVerse.entity.Cart;
import com.example.BookVerse.entity.CartItem;
import com.example.BookVerse.entity.User;
import com.example.BookVerse.exception.AppException;
import com.example.BookVerse.exception.ErrorCode;
import com.example.BookVerse.repository.BookRepository;
import com.example.BookVerse.repository.CartItemRepository;
import com.example.BookVerse.repository.CartRepository;
import com.example.BookVerse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    /** Lấy hoặc tạo giỏ hàng cho user */
    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }

    public CartResponse getCart(String userId) {
        return cartRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(() -> CartResponse.builder()
                        .items(List.of())
                        .totalAmount(0L)
                        .totalItems(0)
                        .build());
    }

    @Transactional
    public CartResponse addToCart(String userId, AddToCartRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));

        // Kiểm tra tồn kho
        if (book.getStock() == null || book.getStock() < request.getQuantity()) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                    "So luong trong kho khong du. Con lai: " + (book.getStock() == null ? 0 : book.getStock()));
        }

        Cart cart = getOrCreateCart(user);

        // Nếu sách đã có trong giỏ thì cộng thêm số lượng
        CartItem item = cartItemRepository.findByCartIdAndBookId(cart.getId(), book.getId())
                .orElse(null);

        if (item != null) {
            int newQty = item.getQuantity() + request.getQuantity();
            if (book.getStock() < newQty) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                        "Tong so luong vuot qua ton kho. Con lai: " + book.getStock());
            }
            item.setQuantity(newQty);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .book(book)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
        }

        return toResponse(cartRepository.findByUserId(userId).orElse(cart));
    }

    @Transactional
    public CartResponse updateQuantity(String userId, String cartItemId, UpdateCartItemRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        // Kiểm tra item thuộc cart của user
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Book book = item.getBook();
        if (book.getStock() == null || book.getStock() < request.getQuantity()) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                    "So luong trong kho khong du. Con lai: " + (book.getStock() == null ? 0 : book.getStock()));
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeFromCart(String userId, String cartItemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        return toResponse(cartRepository.findByUserId(userId).orElse(cart));
    }

    @Transactional
    public void clearCart(String userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        long total = items.stream()
                .mapToLong(i -> i.getSubtotal() == null ? 0L : i.getSubtotal())
                .sum();

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(items)
                .totalAmount(total)
                .totalItems(items.size())
                .build();
    }

    private CartItemResponse toItemResponse(CartItem item) {
        Book b = item.getBook();
        long price = b.getPrice() == null ? 0L : b.getPrice();
        return CartItemResponse.builder()
                .cartItemId(item.getId())
                .bookId(b.getId())
                .title(b.getTitle())
                .author(b.getAuthor())
                .price(b.getPrice())
                .quantity(item.getQuantity())
                .subtotal(price * item.getQuantity())
                .availableStock(b.getStock())
                .build();
    }
}
