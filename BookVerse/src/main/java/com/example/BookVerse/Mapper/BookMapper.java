package com.example.BookVerse.Mapper;

import com.example.BookVerse.dto.request.BookCreateRequest;
import com.example.BookVerse.dto.request.BookUpdateRequest;
import com.example.BookVerse.dto.response.BookRespone;
import com.example.BookVerse.entity.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BookMapper {
    Book toBook (BookCreateRequest request);
    BookRespone toBookRespone (Book book);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "coverPath", ignore = true)
    void updateBook(@MappingTarget Book book, BookUpdateRequest request);
}
