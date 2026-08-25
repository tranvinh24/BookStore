package com.example.BookVerse.service;

import com.example.BookVerse.Mapper.BookMapper;
import com.example.BookVerse.dto.request.BookCreateRequest;
import com.example.BookVerse.dto.request.BookUpdateRequest;
import com.example.BookVerse.dto.response.BookPageResponse;
import com.example.BookVerse.dto.response.BookRespone;
import com.example.BookVerse.entity.Book;
import com.example.BookVerse.exception.AppException;
import com.example.BookVerse.exception.ErrorCode;
import com.example.BookVerse.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Service xử lý nghiệp vụ quản lý sách (CRUD), tìm kiếm, phân trang
 * và phối hợp với ImageService để xử lý ảnh bìa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final BookMapper     bookMapper;
    private final ImageService   imageService;

    // ─────────────────────────────────────────────────────────────
    // CRUD operations
    // ─────────────────────────────────────────────────────────────

    /**
     * Thêm sách mới, có thể kèm ảnh bìa.
     *
     * @param request   Thông tin sách từ client
     * @param coverFile File ảnh bìa (tùy chọn)
     */
    @Transactional
    public BookRespone addBook(BookCreateRequest request, MultipartFile coverFile) {
        Book book = bookMapper.toBook(request);
        book = bookRepository.save(book);  // lưu trước để có ID

        if (coverFile != null && !coverFile.isEmpty()) {
            try {
                String coverPath = imageService.processAndSave(coverFile, book.getId());
                book.setCoverPath(coverPath);
                book = bookRepository.save(book);
            } catch (IOException e) {
                log.error("Lỗi khi xử lý ảnh bìa cho sách id={}: {}", book.getId(), e.getMessage());
                throw new AppException(ErrorCode.FILE_STORAGE_ERROR, "Không thể lưu ảnh bìa: " + e.getMessage());
            }
        }

        return bookMapper.toBookRespone(book);
    }

    /**
     * Lấy thông tin chi tiết một sách theo ID.
     */
    public BookRespone getBook(String id) {
        Book book = findBookOrThrow(id);
        return bookMapper.toBookRespone(book);
    }

    /**
     * Lấy danh sách sách có phân trang, lọc và sắp xếp.
     */
    public BookPageResponse getBooks(
            int page, int size,
            String sortBy, String sortDir,
            String category, Integer year,
            Long minPrice, Long maxPrice) {

        Sort sort = buildSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);

        String catFilter = (category != null && !category.isBlank()) ? category.trim() : null;
        Page<Book> bookPage = bookRepository.findWithFilters(catFilter, year, minPrice, maxPrice, pageable);

        return toPageResponse(bookPage);
    }

    /** Lấy danh sách tất cả thể loại sách có trong hệ thống */
    public List<String> getCategories() {
        return bookRepository.findAllCategories();
    }

    /**
     * Tìm kiếm sách theo từ khóa (tên sách hoặc tác giả) có phân trang.
     */
    public BookPageResponse searchBooks(
            String q, String category,
            Long minPrice, Long maxPrice,
            Integer year,
            int page, int size,
            String sortBy, String sortDir) {

        Sort sort = buildSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);

        String keyword = (q != null && !q.isBlank()) ? q.trim() : null;
        String catFilter = (category != null && !category.isBlank()) ? category.trim() : null;
        Page<Book> bookPage = bookRepository.searchBooks(keyword, catFilter, minPrice, maxPrice, year, pageable);
        return toPageResponse(bookPage);
    }

    /**
     * Cập nhật thông tin sách, có thể thay ảnh bìa mới.
     */
    @Transactional
    public BookRespone updateBook(String id, BookUpdateRequest request, MultipartFile coverFile) {
        Book book = findBookOrThrow(id);
        bookMapper.updateBook(book, request);

        if (coverFile != null && !coverFile.isEmpty()) {
            try {
                imageService.deleteImages(book.getCoverPath());
                String newCoverPath = imageService.processAndSave(coverFile, book.getId());
                book.setCoverPath(newCoverPath);
            } catch (IOException e) {
                log.error("Lỗi khi cập nhật ảnh bìa cho sách id={}: {}", id, e.getMessage());
                throw new AppException(ErrorCode.FILE_STORAGE_ERROR, "Không thể cập nhật ảnh bìa: " + e.getMessage());
            }
        }

        return bookMapper.toBookRespone(bookRepository.save(book));
    }

    /**
     * Xóa sách và toàn bộ ảnh bìa liên quan.
     */
    @Transactional
    public void deleteBook(String id) {
        Book book = findBookOrThrow(id);
        imageService.deleteImages(book.getCoverPath());
        bookRepository.delete(book);
        log.info("Đã xóa sách id={}", id);
    }

    // ─────────────────────────────────────────────────────────────
    // Cover image serving
    // ─────────────────────────────────────────────────────────────

    /**
     * Lấy file ảnh bìa và stream về client.
     *
     * @param id   ID sách
     * @param size Kích thước: "thumbnail", "medium", "large"
     */
    public ResponseEntity<Resource> getCoverFile(String id, String size) {
        Book book = findBookOrThrow(id);

        if (book.getCoverPath() == null || book.getCoverPath().isBlank()) {
            throw new AppException(ErrorCode.FILE_NOT_FOUND, "Sách này chưa có ảnh bìa");
        }

        Path imagePath = imageService.resolveImagePath(book.getCoverPath(), size);
        Resource resource = new FileSystemResource(imagePath);

        if (!resource.exists()) {
            throw new AppException(ErrorCode.FILE_NOT_FOUND, "File ảnh không tồn tại trên server");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")  // cache 1 ngày
                .body(resource);
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────

    private Book findBookOrThrow(String id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND, "Không tìm thấy sách với id: " + id));
    }

    /**
     * Xây dựng Sort từ tên field và chiều sắp xếp.
     * Chỉ cho phép sort theo các field hợp lệ để tránh lỗi query.
     */
    private Sort buildSort(String sortBy, String sortDir) {
        List<String> allowedFields = List.of("title", "year", "rating", "author");
        String field = allowedFields.contains(sortBy) ? sortBy : "title";

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return Sort.by(direction, field);
    }

    /**
     * Chuyển Page<Book> sang BookPageResponse DTO.
     */
    private BookPageResponse toPageResponse(Page<Book> page) {
        List<BookRespone> content = page.getContent()
                .stream()
                .map(bookMapper::toBookRespone)
                .toList();

        return BookPageResponse.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
