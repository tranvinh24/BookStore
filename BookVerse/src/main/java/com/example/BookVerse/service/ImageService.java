package com.example.BookVerse.service;

import com.example.BookVerse.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Service xử lý luồng upload ảnh bìa sách.
 * Delegate việc lưu trữ sang CloudinaryService (cloud) thay vì local disk.
 *
 * ImageService chỉ đảm nhiệm:
 *   1. Validate MIME type
 *   2. Đọc file thành BufferedImage
 *   3. Gọi CloudinaryService để upload
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final AppProperties     appProperties;
    private final CloudinaryService cloudinaryService;

    private static final List<String> ALLOWED_TYPES =
            List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    // ─────────────────────────────────────────────────────────────

    /**
     * Upload ảnh bìa từ MultipartFile lên Cloudinary.
     *
     * @param file   File ảnh từ client
     * @param bookId ID sách
     * @return coverPath lưu vào DB (public_id base trên Cloudinary)
     */
    public String processAndSave(MultipartFile file, String bookId) throws IOException {
        validateFileType(file);

        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new IllegalArgumentException(
                "Không thể đọc file ảnh. Hãy kiểm tra định dạng file.");
        }

        return cloudinaryService.uploadCoverImages(originalImage, bookId);
    }

    /**
     * Tải ảnh từ URL rồi upload lên Cloudinary.
     *
     * @param imageUrl URL công khai của ảnh
     * @param bookId   ID sách
     * @return coverPath hoặc null nếu không tải được
     */
    public String processAndSaveFromUrl(String imageUrl, String bookId) {
        if (imageUrl == null || imageUrl.isBlank()) return null;

        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl.trim()))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<InputStream> response =
                    client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                log.warn("Không thể tải ảnh từ URL {}: HTTP {}", imageUrl, response.statusCode());
                return null;
            }

            try (InputStream in = response.body()) {
                BufferedImage originalImage = ImageIO.read(in);
                if (originalImage == null) {
                    log.warn("URL {} không trả về ảnh hợp lệ", imageUrl);
                    return null;
                }
                return cloudinaryService.uploadCoverImages(originalImage, bookId);
            }
        } catch (Exception e) {
            log.warn("Lỗi khi tải ảnh từ URL {} cho bookId={}: {}", imageUrl, bookId, e.getMessage());
            return null;
        }
    }

    /**
     * Xóa ảnh bìa khỏi Cloudinary khi xóa sách.
     */
    public void deleteImages(String coverPath) {
        cloudinaryService.deleteCoverImages(coverPath);
    }

    /**
     * Trả về URL ảnh bìa từ Cloudinary theo size.
     * Được dùng để serve ảnh thay vì đọc file từ disk.
     */
    public String resolveImageUrl(String coverPath, String size) {
        return cloudinaryService.getCoverUrl(coverPath, size);
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
