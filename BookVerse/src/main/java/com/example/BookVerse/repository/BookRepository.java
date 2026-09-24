package com.example.BookVerse.repository;

import com.example.BookVerse.entity.Book;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, String> {

    /**
     * Lay sach va khoa row voi PESSIMISTIC_WRITE trong transaction.
     * Dung khi tru ton kho de tranh race condition (overselling).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findByIdWithPessimisticLock(@Param("id") String id);

    /**
     * Tim kiem nang cao: keyword (title/author/isbn), category, khoang gia, nam xb.
     * Tat ca tham so co the null — khi null thi bo qua dieu kien do.
     */
    @Query("""
        SELECT b FROM Book b
        WHERE (:q IS NULL OR
               LOWER(b.title)  LIKE LOWER(CONCAT('%', :q, '%')) OR
               LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%')) OR
               LOWER(b.isbn)   LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:category IS NULL OR LOWER(b.category) = LOWER(:category))
          AND (:minPrice IS NULL OR b.price >= :minPrice)
          AND (:maxPrice IS NULL OR b.price <= :maxPrice)
          AND (:year IS NULL OR b.year = :year)
        """)
    Page<Book> searchBooks(
            @Param("q")        String  q,
            @Param("category") String  category,
            @Param("minPrice") Long    minPrice,
            @Param("maxPrice") Long    maxPrice,
            @Param("year")     Integer year,
            Pageable pageable
    );

    /**
     * Lay danh sach sach co loc theo category va/hoac year.
     */
    @Query("""
        SELECT b FROM Book b
        WHERE (:category IS NULL OR LOWER(b.category) = LOWER(:category))
          AND (:year     IS NULL OR b.year = :year)
          AND (:minPrice IS NULL OR b.price >= :minPrice)
          AND (:maxPrice IS NULL OR b.price <= :maxPrice)
        """)
    Page<Book> findWithFilters(
            @Param("category") String  category,
            @Param("year")     Integer year,
            @Param("minPrice") Long    minPrice,
            @Param("maxPrice") Long    maxPrice,
            Pageable pageable
    );

    /** Lay tat ca cac category duy nhat de hien thi bo loc */
    @Query("SELECT DISTINCT b.category FROM Book b WHERE b.category IS NOT NULL ORDER BY b.category")
    List<String> findAllCategories();

    /**
     * Lay sach co ton kho thap (stock <= threshold), sap xep tang dan theo stock.
     * Dung query DB thay vi loadAll + filter Java.
     */
    @Query("SELECT b FROM Book b WHERE b.stock IS NOT NULL AND b.stock <= :threshold ORDER BY b.stock ASC")
    Page<Book> findLowStockBooks(@Param("threshold") int threshold, Pageable pageable);
}