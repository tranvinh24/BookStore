package com.example.BookVerse.Mapper;

import com.example.BookVerse.Dto.Request.BookAddRequest;
import com.example.BookVerse.Dto.Request.BookUpdateRequest;
import com.example.BookVerse.Dto.Respone.BookRespone;
import com.example.BookVerse.Entity.Book;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BookMapper {
    Book toBook (BookAddRequest request);
    BookRespone toBookRespone (Book book);
    void updatBook(@MappingTarget Book book, BookUpdateRequest request);
}
