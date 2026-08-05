package com.example.BookVerse.service;

import com.example.BookVerse.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Service xử lý việc upload, resize, lưu và xóa ảnh bìa sách.
 *
 * Cấu trúc thư mục lưu ảnh:
 *   uploads/covers/{yyyy}/{MM}/{bookId}-thumbnail.webp  (200px)
 *   uploads/covers/{yyyy}/{MM}/{bookId}-medium.webp     (500px)
 *   uploads/covers/{yyyy}/{MM}/{bookId}-large.webp      (1200px)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final AppProperties appProperties;

    /**
     * Các kích thước cần tạo: tên size → chiều rộng (px).
     * Chiều cao tự động giữ tỉ lệ gốc.
     */
    private static final Map<String, Integer> SIZES = new LinkedHashMap<>() {{
        put("thumbnail", 200);
        put("medium",    500);
        put("large",     1200);
    }};

    /**
     * Xử lý toàn bộ luồng upload ảnh:
     *  1. Validate MIME type
     *  2. Tạo thư mục theo yyyy/MM
     *  3. Resize ảnh gốc thành 3 kích thước và lưu dạng WebP
     *
     * @param file   File ảnh được upload từ client
     * @param bookId ID của sách (dùng làm tên file để đảm bảo unique)
     * @return coverPath dạng "yyyy/MM/bookId" — lưu vào DB để sau này truy xuất
     * @throws IOException          khi có lỗi đọc/ghi file
     * @throws IllegalArgumentException khi file không hợp lệ
     */
    public String processAndSave(MultipartFile file, String bookId) throws IOException {
        // 1. Validate MIME type
        validateFileType(file);

        // 2. Tạo đường dẫn thư mục theo năm/tháng hiện tại
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        Path uploadDir = Paths.get(appProperties.getDir(), datePath);
        Files.createDirectories(uploadDir);  // tạo thư mục nếu chưa tồn tại

        // 3. Đọc ảnh gốc thành BufferedImage
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new IllegalArgumentException("File không phải định dạng ảnh hợp lệ.");
        }

        // 4. Resize và lưu từng kích thước dưới dạng WebP
        for (Map.Entry<String, Integer> entry : SIZES.entrySet()) {
            String sizeName = entry.getKey();  // "thumbnail", "medium", "large"
            int targetWidth  = entry.getValue(); // 200, 500, 1200

            // Tên file: {bookId}-{sizeName}.webp
            String fileName   = bookId + "-" + sizeName + ".webp";
            Path   outputPath = uploadDir.resolve(fileName);

            Thumbnails.of(originalImage)
                    .width(targetWidth)         // giữ tỉ lệ chiều cao theo chiều rộng
                    .outputFormat("webp")
                    .toFile(outputPath.toFile());

            log.info("Saved {} image: {}", sizeName, outputPath);
        }

        // 5. Trả về base path (không kèm size/extension)
        //    Ví dụ: "2026/08/abc-123"
        return datePath + "/" + bookId;
    }

    /**
     * Xóa tất cả 3 file ảnh (thumbnail, medium, large) khi xóa sách.
     *
     * @param coverPath Giá trị coverPath lưu trong DB (ví dụ: "2026/08/abc-123")
     */
    public void deleteImages(String coverPath) {
        if (coverPath == null || coverPath.isBlank()) return;

        SIZES.keySet().forEach(size -> {
            Path filePath = Paths.get(appProperties.getDir(), coverPath + "-" + size + ".webp");
            try {
                boolean deleted = Files.deleteIfExists(filePath);
                if (deleted) log.info("Deleted image: {}", filePath);
            } catch (IOException e) {
                log.warn("Could not delete image: {}", filePath, e);
            }
        });
    }

    /**
     * Lấy đường dẫn đầy đủ tới file ảnh theo size.
     *
     * @param coverPath Giá trị coverPath từ DB (ví dụ: "2026/08/abc-123")
     * @param size      Kích thước: "thumbnail", "medium", "large"
     * @return Path tuyệt đối tới file ảnh
     */
    public Path resolveImagePath(String coverPath, String size) {
        // Validate size name để tránh path traversal attack
        if (!SIZES.containsKey(size)) {
            throw new IllegalArgumentException(
                "Size không hợp lệ. Chọn một trong: " + String.join(", ", SIZES.keySet())
            );
        }
        return Paths.get(appProperties.getDir(), coverPath + "-" + size + ".webp");
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Kiểm tra MIME type của file có nằm trong danh sách cho phép không.
     */
    private void validateFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !appProperties.getAllowedTypeList().contains(contentType)) {
            throw new IllegalArgumentException(
                "Định dạng file không được hỗ trợ: " + contentType +
                ". Chỉ chấp nhận: " + appProperties.getAllowedTypes()
            );
        }
    }
}
