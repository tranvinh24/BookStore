package com.example.BookVerse.service;

import com.example.BookVerse.dto.request.BookCreateRequest;
import com.example.BookVerse.dto.request.BookUpdateRequest;
import com.example.BookVerse.dto.response.BookRespone;
import com.example.BookVerse.entity.Book;
import com.example.BookVerse.Mapper.BookMapper;
import com.example.BookVerse.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Service xử lý nghiệp vụ quản lý sách (CRUD) và phối hợp với ImageService.
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
     * @return BookResponse chứa thông tin sách đã lưu
     */
    @Transactional
    public BookRespone addBook(BookCreateRequest request, MultipartFile coverFile) {
        // 1. Map DTO → Entity và lưu để lấy ID (UUID do DB sinh)
        Book book = bookMapper.toBook(request);
        book = bookRepository.save(book);

        // 2. Xử lý ảnh nếu client có upload
        if (coverFile != null && !coverFile.isEmpty()) {
            try {
                String coverPath = imageService.processAndSave(coverFile, book.getId());
                book.setCoverPath(coverPath);
                book = bookRepository.save(book);  // cập nhật lại coverPath
            } catch (IOException e) {
                log.error("Lỗi khi xử lý ảnh bìa cho sách id={}: {}", book.getId(), e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể lưu ảnh bìa: " + e.getMessage());
            }
        }

        return bookMapper.toBookRespone(book);
    }

    /**
     * Lấy danh sách tất cả sách.
     */
    public List<BookRespone> getBooks() {
        return bookRepository.findAll()
                .stream()
                .map(bookMapper::toBookRespone)
                .toList();
    }

    /**
     * Lấy thông tin chi tiết một sách theo ID.
     */
    public BookRespone getBook(String id) {
        Book book = findBookOrThrow(id);
        return bookMapper.toBookRespone(book);
    }

    /**
     * Cập nhật thông tin sách, có thể thay ảnh bìa mới.
     *
     * @param id        ID sách cần cập nhật
     * @param request   Thông tin cập nhật từ client
     * @param coverFile File ảnh bìa mới (tùy chọn)
     */
    @Transactional
    public BookRespone updateBook(String id, BookUpdateRequest request, MultipartFile coverFile) {
        Book book = findBookOrThrow(id);

        // 1. Cập nhật các field thông tin sách
        bookMapper.updateBook(book, request);

        // 2. Thay ảnh bìa nếu có upload file mới
        if (coverFile != null && !coverFile.isEmpty()) {
            try {
                // Xóa ảnh cũ trước khi lưu ảnh mới
                imageService.deleteImages(book.getCoverPath());

                String newCoverPath = imageService.processAndSave(coverFile, book.getId());
                book.setCoverPath(newCoverPath);
            } catch (IOException e) {
                log.error("Lỗi khi cập nhật ảnh bìa cho sách id={}: {}", id, e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể cập nhật ảnh bìa: " + e.getMessage());
            }
        }

        return bookMapper.toBookRespone(bookRepository.save(book));
    }

    /**
     * Xóa sách và toàn bộ ảnh bìa liên quan.
     *
     * @param id ID sách cần xóa
     */
    @Transactional
    public void deleteBook(String id) {
        Book book = findBookOrThrow(id);

        // Xóa các file ảnh khỏi disk trước
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
     * @return ResponseEntity chứa file ảnh WebP kèm cache headers
     */
    public ResponseEntity<Resource> getCoverFile(String id, String size) {
        Book book = findBookOrThrow(id);

        if (book.getCoverPath() == null || book.getCoverPath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Sách này chưa có ảnh bìa.");
        }

        Path imagePath = imageService.resolveImagePath(book.getCoverPath(), size);
        Resource resource = new FileSystemResource(imagePath);

        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "File ảnh không tồn tại trên server.");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/webp"))
                // Cache ảnh tại browser trong 1 ngày (86400 giây)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(resource);
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────

    private Book findBookOrThrow(String id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Không tìm thấy sách với id: " + id));
    }
}
