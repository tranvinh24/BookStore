package com.example.BookVerse.Mapper;

import com.example.BookVerse.Dto.Request.BookAddRequest;
import com.example.BookVerse.Dto.Request.BookUpdateRequest;
import com.example.BookVerse.Dto.Respone.BookRespone;
import com.example.BookVerse.Entity.Book;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-04T17:41:58+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25 (Oracle Corporation)"
)
@Component
public class BookMapperImpl implements BookMapper {

    @Override
    public Book toBook(BookAddRequest request) {
        if ( request == null ) {
            return null;
        }

        Book book = new Book();

        book.setId( request.getId() );
        book.setName( request.getName() );
        book.setAuthor( request.getAuthor() );

        return book;
    }

    @Override
    public BookRespone toBookRespone(Book book) {
        if ( book == null ) {
            return null;
        }

        BookRespone.BookResponeBuilder bookRespone = BookRespone.builder();

        bookRespone.id( book.getId() );
        bookRespone.name( book.getName() );
        bookRespone.author( book.getAuthor() );

        return bookRespone.build();
    }

    @Override
    public void updatBook(Book book, BookUpdateRequest request) {
        if ( request == null ) {
            return;
        }

        book.setId( request.getId() );
        book.setName( request.getName() );
        book.setAuthor( request.getAuthor() );
    }
}
