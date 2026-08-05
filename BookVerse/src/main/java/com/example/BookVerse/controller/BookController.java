package com.example.BookVerse.Controller;

import com.example.BookVerse.Dto.Request.BookCreateRequest;
import com.example.BookVerse.Dto.Request.BookUpdateRequest;
import com.example.BookVerse.Dto.Respone.BookRespone;
import com.example.BookVerse.Entity.Book;
import com.example.BookVerse.Service.BookService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BookController {
    @Autowired
    BookService bookService;
    @PostMapping("/api/books")
    public BookRespone addBook(@Valid @RequestBody BookCreateRequest request){
        return bookService.addBook(request);
    }
    @GetMapping("/api/books")
    public List<BookRespone> getBooks(){
        return bookService.getBooks();
    }
    @GetMapping("/api/books/{id}")
    public BookRespone getBook(@PathVariable("id") String id){
        return bookService.getBook(id);
    }
    @PutMapping("/api/books/{id}")
    public BookRespone updateBook(@PathVariable("id") String id,@Valid @RequestBody BookUpdateRequest request){
        return bookService.updateBook(id,request);
    }
    @DeleteMapping("/api/books/{id}")
    public String deleteBook(@PathVariable("id") String id){
        bookService.deleteBook(id);
        return "xoa thanh cong";
    }
}
