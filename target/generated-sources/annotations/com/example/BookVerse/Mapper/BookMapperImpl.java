package com.example.BookVerse.Mapper;

import com.example.BookVerse.Dto.Request.BookCreateRequest;
import com.example.BookVerse.Dto.Request.BookUpdateRequest;
import com.example.BookVerse.Dto.Respone.BookRespone;
import com.example.BookVerse.Entity.Book;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-05T15:32:26+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.11 (Microsoft)"
)
@Component
public class BookMapperImpl implements BookMapper {

    @Override
    public Book toBook(BookCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        Book book = new Book();

        book.setId( request.getId() );
        book.setTitle( request.getTitle() );
        book.setAuthor( request.getAuthor() );
        book.setIsbn( request.getIsbn() );
        book.setYear( request.getYear() );
        book.setCategory( request.getCategory() );
        book.setRating( request.getRating() );
        book.setDescription( request.getDescription() );
        book.setCoverPath( request.getCoverPath() );

        return book;
    }

    @Override
    public BookRespone toBookRespone(Book book) {
        if ( book == null ) {
            return null;
        }

        BookRespone.BookResponeBuilder bookRespone = BookRespone.builder();

        bookRespone.id( book.getId() );
        bookRespone.title( book.getTitle() );
        bookRespone.author( book.getAuthor() );
        bookRespone.isbn( book.getIsbn() );
        bookRespone.year( book.getYear() );
        bookRespone.category( book.getCategory() );
        bookRespone.rating( book.getRating() );
        bookRespone.description( book.getDescription() );
        bookRespone.coverPath( book.getCoverPath() );

        return bookRespone.build();
    }

    @Override
    public void updateBook(Book book, BookUpdateRequest request) {
        if ( request == null ) {
            return;
        }

        book.setTitle( request.getTitle() );
        book.setAuthor( request.getAuthor() );
        book.setIsbn( request.getIsbn() );
        book.setYear( request.getYear() );
        book.setCategory( request.getCategory() );
        book.setRating( request.getRating() );
        book.setDescription( request.getDescription() );
        book.setCoverPath( request.getCoverPath() );
    }
}
