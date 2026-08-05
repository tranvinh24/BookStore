package com.example.BookVerse.Mapper;

import com.example.BookVerse.dto.request.BookCreateRequest;
import com.example.BookVerse.dto.request.BookUpdateRequest;
import com.example.BookVerse.dto.response.BookRespone;
import com.example.BookVerse.entity.Book;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BookMapper {
    Book toBook (BookCreateRequest request);
    BookRespone toBookRespone (Book book);
    void updateBook(@MappingTarget Book book, BookUpdateRequest request);
}
