package com.example.BookVerse.service;

import com.example.BookVerse.dto.request.AddToWishlistRequest;
import com.example.BookVerse.dto.response.WishlistResponse;
import com.example.BookVerse.entity.Book;
import com.example.BookVerse.entity.User;
import com.example.BookVerse.entity.Wishlist;
import com.example.BookVerse.exception.AppException;
import com.example.BookVerse.exception.ErrorCode;
import com.example.BookVerse.repository.BookRepository;
import com.example.BookVerse.repository.UserRepository;
import com.example.BookVerse.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public List<WishlistResponse> getWishlist(String userId) {
        return wishlistRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WishlistResponse addToWishlist(String userId, AddToWishlistRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));

        if (wishlistRepository.existsByUserIdAndBookId(userId, book.getId())) {
            throw new AppException(ErrorCode.WISHLIST_ALREADY_EXISTS);
        }

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .book(book)
                .build();

        return toResponse(wishlistRepository.save(wishlist));
    }

    @Transactional
    public void removeFromWishlist(String userId, String bookId) {
        if (!wishlistRepository.existsByUserIdAndBookId(userId, bookId)) {
            throw new AppException(ErrorCode.WISHLIST_NOT_FOUND);
        }
        wishlistRepository.deleteByUserIdAndBookId(userId, bookId);
    }

    private WishlistResponse toResponse(Wishlist w) {
        Book b = w.getBook();
        return WishlistResponse.builder()
                .id(w.getId())
                .bookId(b.getId())
                .title(b.getTitle())
                .author(b.getAuthor())
                .category(b.getCategory())
                .price(b.getPrice())
                .stock(b.getStock())
                .coverPath(b.getCoverPath())
                .addedAt(w.getAddedAt())
                .build();
    }
}
