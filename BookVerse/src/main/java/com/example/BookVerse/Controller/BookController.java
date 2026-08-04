package com.example.BookVerse.Controller;

import com.example.BookVerse.Dto.Request.BookAddRequest;
import com.example.BookVerse.Entity.Book;
import com.example.BookVerse.Repository.BookRepository;
import com.example.BookVerse.Service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BookController {
    @Autowired
    BookService bookService;
    @PostMapping
    public Book addBook(@RequestBody Book book){
        return bookService.addBook(book);
    }
    @GetMapping
    public List<Book> getBooks(){
        return bookService.getBooks();
    }
    @GetMapping("/{id}")
    public Book getBook(@PathVariable("id") String id){
        return bookService.getBook(id);
    }
    @PutMapping
    public Book updateBook(@RequestBody Book book){
        return bookService.updateBook(book);
    }
    @DeleteMapping("{id}")
    public String deleteBook(@PathVariable("id") String id){
        bookService.deleteBook(id);
        return "xoa thanh cong";
    }
}
