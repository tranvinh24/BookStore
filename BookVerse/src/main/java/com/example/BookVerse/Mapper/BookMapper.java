package com.example.BookVerse.Mapper;

import com.example.BookVerse.dto.request.BookCreateRequest;
import com.example.BookVerse.dto.request.BookUpdateRequest;
import com.example.BookVerse.dto.response.BookRespone;
import com.example.BookVerse.entity.Book;
import org.springframework.stereotype.Component;

/**
 * Manual mapper thay thế MapStruct — tránh annotation processing issues trên Spring Boot 4.x.
 */
@Component
public class BookMapper {

    public Book toBook(BookCreateRequest request) {
        if (request == null) return null;
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setYear(request.getYear());
        book.setCategory(request.getCategory());
        book.setRating(request.getRating());
        book.setDescription(request.getDescription());
        book.setPrice(request.getPrice());
        book.setStock(request.getStock());
        return book;
    }

    public BookRespone toBookRespone(Book book) {
        if (book == null) return null;
        return BookRespone.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .year(book.getYear())
                .category(book.getCategory())
                .rating(book.getRating())
                .description(book.getDescription())
                .coverPath(book.getCoverPath())
                .price(book.getPrice())
                .stock(book.getStock())
                .build();
    }

    public void updateBook(Book book, BookUpdateRequest request) {
        if (request == null) return;
        if (request.getTitle()       != null) book.setTitle(request.getTitle());
        if (request.getAuthor()      != null) book.setAuthor(request.getAuthor());
        if (request.getIsbn()        != null) book.setIsbn(request.getIsbn());
        if (request.getYear()        != null) book.setYear(request.getYear());
        if (request.getCategory()    != null) book.setCategory(request.getCategory());
        if (request.getRating()      != null) book.setRating(request.getRating());
        if (request.getDescription() != null) book.setDescription(request.getDescription());
        if (request.getPrice()       != null) book.setPrice(request.getPrice());
        if (request.getStock()       != null) book.setStock(request.getStock());
        // coverPath không update ở đây — được xử lý riêng qua ImageService
    }
}
