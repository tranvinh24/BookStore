package com.example.BookVerse.repository;

import com.example.BookVerse.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, String> {

    /**
     * Tìm kiếm full-text theo title hoặc author (không phân biệt hoa thường),
     * có thể lọc thêm theo category (null = lấy tất cả).
     *
     * @param q        Từ khóa tìm kiếm
     * @param category Thể loại cần lọc, null = không lọc
     * @param pageable Thông tin phân trang và sắp xếp
     */
    @Query("""
        SELECT b FROM Book b
        WHERE (:q IS NULL OR
               LOWER(b.title)  LIKE LOWER(CONCAT('%', :q, '%')) OR
               LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:category IS NULL OR LOWER(b.category) = LOWER(:category))
        """)
    Page<Book> searchBooks(
            @Param("q")        String q,
            @Param("category") String category,
            Pageable pageable
    );

    /**
     * Lấy danh sách sách có lọc theo category và/hoặc year.
     * Null = bỏ qua điều kiện đó.
     *
     * @param category Thể loại, null = tất cả
     * @param year     Năm xuất bản, null = tất cả
     * @param pageable Thông tin phân trang và sắp xếp
     */
    @Query("""
        SELECT b FROM Book b
        WHERE (:category IS NULL OR LOWER(b.category) = LOWER(:category))
          AND (:year     IS NULL OR b.year = :year)
        """)
    Page<Book> findWithFilters(
            @Param("category") String category,
            @Param("year")     Integer year,
            Pageable pageable
    );
}
