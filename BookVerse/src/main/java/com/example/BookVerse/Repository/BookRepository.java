package com.example.BookVerse.Repository;

import com.example.BookVerse.Entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book,String> {
}
