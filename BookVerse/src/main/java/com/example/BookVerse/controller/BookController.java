package com.example.BookVerse.controller;

import com.example.BookVerse.dto.request.BookCreateRequest;
import com.example.BookVerse.dto.request.BookUpdateRequest;
import com.example.BookVerse.dto.response.BookPageResponse;
import com.example.BookVerse.dto.response.BookRespone;
import com.example.BookVerse.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller quản lý sách và ảnh bìa.
 * Base path: /api/books
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    // ─────────────────────────────────────────────────────────────
    // GET /api/books — danh sách có phân trang, filter, sort
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /api/books?page=0&size=10&sort=title&dir=asc&category=Fiction&year=2020
     *
     * @param page     Số trang, bắt đầu từ 0 (mặc định: 0)
     * @param size     Số sách mỗi trang (mặc định: 10)
     * @param sort     Field sắp xếp: title | year | rating | author (mặc định: title)
     * @param dir      Chiều sắp xếp: asc | desc (mặc định: asc)
     * @param category Lọc theo thể loại (tùy chọn)
     * @param year     Lọc theo năm xuất bản (tùy chọn)
     */
    @GetMapping
    public BookPageResponse getBooks(
            @RequestParam(defaultValue = "0")     int     page,
            @RequestParam(defaultValue = "10")    int     size,
            @RequestParam(defaultValue = "title") String  sort,
            @RequestParam(defaultValue = "asc")   String  dir,
            @RequestParam(required = false)        String  category,
            @RequestParam(required = false)        Integer year) {
        return bookService.getBooks(page, size, sort, dir, category, year);
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/books/search — tìm kiếm full-text
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /api/books/search?q=clean+code&category=Programming&page=0&size=10
     *
     * @param q        Từ khóa tìm kiếm (tên sách hoặc tác giả)
     * @param category Lọc theo thể loại (tùy chọn)
     * @param page     Số trang (mặc định: 0)
     * @param size     Số phần tử mỗi trang (mặc định: 10)
     * @param sort     Field sắp xếp (mặc định: title)
     * @param dir      Chiều sắp xếp (mặc định: asc)
     */
    @GetMapping("/search")
    public BookPageResponse searchBooks(
            @RequestParam(required = false)        String  q,
            @RequestParam(required = false)        String  category,
            @RequestParam(defaultValue = "0")     int     page,
            @RequestParam(defaultValue = "10")    int     size,
            @RequestParam(defaultValue = "title") String  sort,
            @RequestParam(defaultValue = "asc")   String  dir) {
        return bookService.searchBooks(q, category, page, size, sort, dir);
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/books/{id}
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /api/books/{id}
     * Lấy thông tin chi tiết một sách.
     */
    @GetMapping("/{id}")
    public BookRespone getBook(@PathVariable String id) {
        return bookService.getBook(id);
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/books
    // ─────────────────────────────────────────────────────────────

    /**
     * POST /api/books  (Content-Type: application/json)
     * Thêm sách mới không kèm ảnh bìa.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public BookRespone addBook(@Valid @RequestBody BookCreateRequest request) {
        return bookService.addBook(request, null);
    }

    /**
     * POST /api/books  (Content-Type: multipart/form-data)
     * Thêm sách mới kèm upload ảnh bìa.
     *
     * Parts:
     *   - data  : JSON thông tin sách
     *   - cover : File ảnh (JPG/PNG/WebP) — tùy chọn
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public BookRespone addBookWithCover(
            @Valid @RequestPart("data") BookCreateRequest request,
            @RequestPart(value = "cover", required = false) MultipartFile coverFile) {
        return bookService.addBook(request, coverFile);
    }

    // ─────────────────────────────────────────────────────────────
    // PUT /api/books/{id}
    // ─────────────────────────────────────────────────────────────

    /**
     * PUT /api/books/{id}  (Content-Type: application/json)
     * Cập nhật thông tin sách không thay ảnh.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BookRespone updateBook(
            @PathVariable String id,
            @Valid @RequestBody BookUpdateRequest request) {
        return bookService.updateBook(id, request, null);
    }

    /**
     * PUT /api/books/{id}  (Content-Type: multipart/form-data)
     * Cập nhật thông tin sách và thay ảnh bìa.
     *
     * Parts:
     *   - data  : JSON thông tin cập nhật
     *   - cover : File ảnh mới — tùy chọn
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BookRespone updateBookWithCover(
            @PathVariable String id,
            @Valid @RequestPart("data") BookUpdateRequest request,
            @RequestPart(value = "cover", required = false) MultipartFile coverFile) {
        return bookService.updateBook(id, request, coverFile);
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE /api/books/{id}
    // ─────────────────────────────────────────────────────────────

    /**
     * DELETE /api/books/{id}
     * Xóa sách và toàn bộ ảnh bìa.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable String id) {
        bookService.deleteBook(id);
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/books/{id}/cover
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /api/books/{id}/cover?size=large
     * Trả về file ảnh bìa theo kích thước yêu cầu.
     *
     * @param id   ID sách
     * @param size thumbnail (200px) | medium (500px) | large (1200px)
     */
    @GetMapping("/{id}/cover")
    public ResponseEntity<Resource> getCover(
            @PathVariable String id,
            @RequestParam(defaultValue = "medium") String size) {
        return bookService.getCoverFile(id, size);
    }
}
