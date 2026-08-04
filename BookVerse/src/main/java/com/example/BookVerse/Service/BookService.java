package com.example.BookVerse.Service;

import com.example.BookVerse.Dto.Request.BookAddRequest;
import com.example.BookVerse.Dto.Request.BookUpdateRequest;
import com.example.BookVerse.Dto.Respone.BookRespone;
import com.example.BookVerse.Entity.Book;
import com.example.BookVerse.Mapper.BookMapper;
import com.example.BookVerse.Repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {
    @Autowired
    BookRepository bookRepository;
    BookMapper bookMapper;

    public Book addBook(Book book){
        return bookRepository.save(book);
    }
    public List<Book> getBooks(){
        return bookRepository.findAll();
    }
    public Book getBook(String id){
        return bookRepository.findById(id).orElseThrow();
    }
    public Book updateBook(Book book){
        return bookRepository.save(book);
    }
    public void deleteBook(String id){
        bookRepository.deleteById(id);
    }
}
