package com.example.BookVerse.repository;

import com.example.BookVerse.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book,String> {
}
