package com.example.BookVerse.controller;

import com.example.BookVerse.dto.request.BookCreateRequest;
import com.example.BookVerse.dto.request.BookUpdateRequest;
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

import java.util.List;

/**
 * REST Controller quản lý sách và ảnh bìa.
 *
 * Base path: /api/books
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    // ─────────────────────────────────────────────────────────────
    // CRUD Endpoints
    // ─────────────────────────────────────────────────────────────

    /**
     * POST /api/books
     * Thêm sách mới. Hỗ trợ upload ảnh bìa qua multipart/form-data.
     *
     * Body (multipart/form-data):
     *   - data: JSON string chứa thông tin sách (BookCreateRequest)
     *   - cover: File ảnh (JPG / PNG / WebP) — tùy chọn
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public BookRespone addBook(
            @Valid @RequestPart("data") BookCreateRequest request,
            @RequestPart(value = "cover", required = false) MultipartFile coverFile) {
        return bookService.addBook(request, coverFile);
    }

    /**
     * GET /api/books
     * Lấy danh sách tất cả sách.
     */
    @GetMapping
    public List<BookRespone> getBooks() {
        return bookService.getBooks();
    }

    /**
     * GET /api/books/{id}
     * Lấy chi tiết một sách theo ID.
     */
    @GetMapping("/{id}")
    public BookRespone getBook(@PathVariable String id) {
        return bookService.getBook(id);
    }

    /**
     * PUT /api/books/{id}
     * Cập nhật thông tin sách. Hỗ trợ thay ảnh bìa mới.
     *
     * Body (multipart/form-data):
     *   - data: JSON string chứa thông tin cần cập nhật (BookUpdateRequest)
     *   - cover: File ảnh mới — tùy chọn
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BookRespone updateBook(
            @PathVariable String id,
            @Valid @RequestPart("data") BookUpdateRequest request,
            @RequestPart(value = "cover", required = false) MultipartFile coverFile) {
        return bookService.updateBook(id, request, coverFile);
    }

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
    // Cover Image Endpoint
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /api/books/{id}/cover?size=medium
     * Trả về file ảnh bìa của sách theo kích thước yêu cầu.
     *
     * @param id   ID sách
     * @param size Kích thước ảnh: "thumbnail" (200px), "medium" (500px), "large" (1200px)
     *             Mặc định: "medium"
     */
    @GetMapping("/{id}/cover")
    public ResponseEntity<Resource> getCover(
            @PathVariable String id,
            @RequestParam(defaultValue = "medium") String size) {
        return bookService.getCoverFile(id, size);
    }
}
