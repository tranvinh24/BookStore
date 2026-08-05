package com.example.BookVerse.service;

import com.example.BookVerse.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service xử lý việc upload, resize, lưu và xóa ảnh bìa sách.
 *
 * Cấu trúc thư mục lưu ảnh:
 *   uploads/covers/{yyyy}/{MM}/{bookId}-thumbnail.jpg  (200px)
 *   uploads/covers/{yyyy}/{MM}/{bookId}-medium.jpg     (500px)
 *   uploads/covers/{yyyy}/{MM}/{bookId}-large.jpg      (1200px)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final AppProperties appProperties;

    /** Các kích thước cần tạo: tên size → chiều rộng (px) */
    private static final Map<String, Integer> SIZES = new LinkedHashMap<>() {{
        put("thumbnail", 200);
        put("medium",    500);
        put("large",     1200);
    }};

    /** Định dạng output — JPEG được Java hỗ trợ native, chất lượng tốt */
    private static final String OUTPUT_FORMAT    = "jpg";
    private static final String OUTPUT_EXTENSION = ".jpg";

    /** MIME type được phép upload */
    private static final List<String> ALLOWED_TYPES =
            List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    // ─────────────────────────────────────────────────────────────

    /**
     * Xử lý toàn bộ luồng upload ảnh:
     *  1. Validate MIME type
     *  2. Tạo thư mục theo yyyy/MM
     *  3. Resize ảnh gốc thành 3 kích thước và lưu dạng JPEG
     *
     * @param file   File ảnh được upload từ client
     * @param bookId ID của sách (dùng làm tên file để đảm bảo unique)
     * @return coverPath dạng "yyyy/MM/bookId" — lưu vào DB để sau này truy xuất
     */
    public String processAndSave(MultipartFile file, String bookId) throws IOException {
        // 1. Validate MIME type
        validateFileType(file);

        // 2. Tạo đường dẫn thư mục tuyệt đối theo năm/tháng hiện tại
        String datePath = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyy/MM"));

        // Dùng đường dẫn tuyệt đối từ working directory để tránh lỗi path
        Path uploadDir = Paths.get(System.getProperty("user.dir"),
                                   appProperties.getDir(), datePath);
        Files.createDirectories(uploadDir);
        log.info("Upload directory: {}", uploadDir.toAbsolutePath());

        // 3. Đọc ảnh gốc thành BufferedImage
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new IllegalArgumentException(
                "Không thể đọc file ảnh. Hãy kiểm tra định dạng file.");
        }

        // 4. Resize và lưu từng kích thước
        for (Map.Entry<String, Integer> entry : SIZES.entrySet()) {
            String sizeName   = entry.getKey();    // "thumbnail", "medium", "large"
            int    targetWidth = entry.getValue(); // 200, 500, 1200

            String fileName   = bookId + "-" + sizeName + OUTPUT_EXTENSION;
            Path   outputPath = uploadDir.resolve(fileName);

            // Thumbnailator giữ tỉ lệ chiều cao tự động theo chiều rộng
            Thumbnails.of(originalImage)
                    .width(targetWidth)
                    .outputFormat(OUTPUT_FORMAT)
                    .outputQuality(0.85)   // 85% quality — cân bằng dung lượng/chất lượng
                    .toFile(outputPath.toFile());

            log.info("Saved {} image ({} px): {}", sizeName, targetWidth, outputPath);
        }

        // 5. Trả về base path lưu vào DB (không kèm extension)
        //    Ví dụ: "2026/08/abc-uuid-123"
        return datePath + "/" + bookId;
    }

    /**
     * Xóa tất cả 3 file ảnh khi xóa sách.
     *
     * @param coverPath Giá trị coverPath từ DB (ví dụ: "2026/08/abc-123")
     */
    public void deleteImages(String coverPath) {
        if (coverPath == null || coverPath.isBlank()) return;

        SIZES.keySet().forEach(size -> {
            Path filePath = Paths.get(System.getProperty("user.dir"),
                    appProperties.getDir(), coverPath + "-" + size + OUTPUT_EXTENSION);
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
     */
    public Path resolveImagePath(String coverPath, String size) {
        if (!SIZES.containsKey(size)) {
            throw new IllegalArgumentException(
                "Size không hợp lệ. Chọn: " + String.join(", ", SIZES.keySet()));
        }
        return Paths.get(System.getProperty("user.dir"),
                appProperties.getDir(), coverPath + "-" + size + OUTPUT_EXTENSION);
    }

    // ─────────────────────────────────────────────────────────────

    private void validateFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                "Định dạng không hỗ trợ: " + contentType +
                ". Chấp nhận: JPEG, PNG, WebP, GIF");
        }
    }
}
