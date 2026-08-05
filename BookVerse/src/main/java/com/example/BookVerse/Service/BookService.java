package com.example.BookVerse.Service;

import com.example.BookVerse.Dto.Request.BookCreateRequest;
import com.example.BookVerse.Dto.Request.BookUpdateRequest;
import com.example.BookVerse.Dto.Respone.BookRespone;
import com.example.BookVerse.Entity.Book;
import com.example.BookVerse.Mapper.BookMapper;
import com.example.BookVerse.Repository.BookRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    final BookRepository  bookRepository;
    final BookMapper bookMapper;

    public BookRespone addBook(@Valid BookCreateRequest request){
        Book book = bookMapper.toBook(request);
        return bookMapper.toBookRespone(bookRepository.save(book));
    }
    public List<BookRespone> getBooks(){
        return bookRepository.findAll().stream().map(bookMapper::toBookRespone).toList();
    }
    public BookRespone getBook(String id){
        return bookMapper.toBookRespone(bookRepository.findById(id).orElseThrow());
    }
    public BookRespone updateBook(String id,@Valid BookUpdateRequest request){
        Book book = bookRepository.findById(id).orElseThrow();
        bookMapper.updateBook(book,request);
        return bookMapper.toBookRespone(bookRepository.save(book));
    }
    public void deleteBook(String id){
        bookRepository.deleteById(id);
    }
}
