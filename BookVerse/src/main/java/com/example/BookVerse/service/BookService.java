package com.example.BookVerse.service;

import com.example.BookVerse.Mapper.BookMapper;
import com.example.BookVerse.dto.request.BookCreateRequest;
import com.example.BookVerse.dto.request.BookUpdateRequest;
import com.example.BookVerse.dto.response.BookImportResult;
import com.example.BookVerse.dto.response.BookPageResponse;
import com.example.BookVerse.dto.response.BookRespone;
import com.example.BookVerse.entity.Book;
import com.example.BookVerse.exception.AppException;
import com.example.BookVerse.exception.ErrorCode;
import com.example.BookVerse.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service xử lý nghiệp vụ quản lý sách (CRUD), tìm kiếm, phân trang
 * và phối hợp với ImageService để xử lý ảnh bìa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository        bookRepository;
    private final BookMapper            bookMapper;
    private final ImageService          imageService;
    private final BookFileParserService bookFileParserService;

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
        String oldCoverPath = book.getCoverPath();

        bookMapper.updateBook(book, request);

        if (coverFile != null && !coverFile.isEmpty()) {
            try {
                if (oldCoverPath != null && !oldCoverPath.isBlank()) {
                    imageService.deleteImages(oldCoverPath);
                }
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

        // Dùng Last-Modified từ file thực tế để browser tái validate mỗi lần.
        // Tránh dùng max-age cố định vì khi admin cập nhật ảnh, browser sẽ dùng cache cũ.
        try {
            long lastModifiedMillis = Files.getLastModifiedTime(imagePath).toMillis();
            String lastModified = DateTimeFormatter.RFC_1123_DATE_TIME.format(
                    ZonedDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(lastModifiedMillis),
                            ZoneOffset.UTC));
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .cacheControl(CacheControl.noCache())   // buộc browser tái validate
                    .header(HttpHeaders.LAST_MODIFIED, lastModified)
                    .body(resource);
        } catch (IOException e) {
            log.warn("Không đọc được last-modified của file ảnh: {}", imagePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .cacheControl(CacheControl.noCache())
                    .body(resource);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Bulk Import from XLSX / CSV & Template Download
    // ─────────────────────────────────────────────────────────────

    /**
     * Nhập sách hàng loạt từ file Excel (.xlsx, .xls) hoặc CSV (.csv).
     */
    @Transactional
    public BookImportResult importBooks(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT, "Vui lòng chọn file Excel hoặc CSV để tải lên");
        }

        try {
            BookFileParserService.ParseResult parseResult = bookFileParserService.parseFile(file);

            List<Book> savedBooks = new java.util.ArrayList<>();
            for (BookFileParserService.ParsedBookItem item : parseResult.getValidItems()) {
                Book book = item.getBook();
                book = bookRepository.save(book);

                if (item.getImageUrl() != null && !item.getImageUrl().isBlank()) {
                    String coverPath = imageService.processAndSaveFromUrl(item.getImageUrl(), book.getId());
                    if (coverPath != null) {
                        book.setCoverPath(coverPath);
                        book = bookRepository.save(book);
                    }
                }
                savedBooks.add(book);
            }

            List<BookRespone> importedResponses = savedBooks.stream()
                    .map(bookMapper::toBookRespone)
                    .toList();

            return BookImportResult.builder()
                    .totalRows(parseResult.getTotalRows())
                    .successCount(savedBooks.size())
                    .failedCount(parseResult.getErrors().size())
                    .importedBooks(importedResponses)
                    .errors(parseResult.getErrors())
                    .build();

        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT, e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi đọc file import sách: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.FILE_STORAGE_ERROR, "Không thể đọc và xử lý file: " + e.getMessage());
        }
    }

    /**
     * Tạo file mẫu nhập sách chuẩn (.xlsx hoặc .csv).
     */
    public ResponseEntity<Resource> generateTemplate(String format) {
        try {
            boolean isCsv = "csv".equalsIgnoreCase(format);
            byte[] content = isCsv ? bookFileParserService.generateCsvTemplate() : bookFileParserService.generateExcelTemplate();
            String filename = isCsv ? "mau_nhap_sach.csv" : "mau_nhap_sach.xlsx";
            MediaType mediaType = isCsv
                    ? MediaType.parseMediaType("text/csv; charset=UTF-8")
                    : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            ByteArrayResource resource = new ByteArrayResource(content);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (IOException e) {
            log.error("Lỗi khi tạo file mẫu sách: {}", e.getMessage());
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không thể tạo file mẫu: " + e.getMessage());
        }
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
