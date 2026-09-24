package com.example.BookVerse.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;

/**
 * Service xử lý upload và xóa ảnh trên Cloudinary.
 * Thay thế ImageService local-disk để hoạt động trên Render (ephemeral filesystem).
 *
 * Cấu trúc public_id trên Cloudinary:
 *   bookverse/covers/{bookId}-thumbnail
 *   bookverse/covers/{bookId}-medium
 *   bookverse/covers/{bookId}-large
 *   bookverse/avatars/{userId}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    private static final Map<String, Integer> COVER_SIZES = Map.of(
            "thumbnail", 200,
            "medium",    500,
            "large",     1200
    );

    private static final String COVER_FOLDER  = "bookverse/covers";
    private static final String AVATAR_FOLDER = "bookverse/avatars";

    // ─────────────────────────────────────────────────────────────
    // Ảnh bìa sách
    // ─────────────────────────────────────────────────────────────

    /**
     * Upload ảnh bìa sách lên Cloudinary (3 kích thước).
     * Trả về base public_id dùng làm coverPath lưu vào DB.
     *
     * @param originalImage BufferedImage gốc đã đọc từ MultipartFile/URL
     * @param bookId        ID sách — dùng làm tên file trên Cloudinary
     * @return coverPath dạng "bookverse/covers/{bookId}" lưu vào DB
     */
    public String uploadCoverImages(BufferedImage originalImage, String bookId) throws IOException {
        for (Map.Entry<String, Integer> entry : COVER_SIZES.entrySet()) {
            String sizeName   = entry.getKey();
            int    targetWidth = entry.getValue();
            String publicId   = COVER_FOLDER + "/" + bookId + "-" + sizeName;

            byte[] resizedBytes = resizeToBytes(originalImage, targetWidth);

            cloudinary.uploader().upload(resizedBytes, ObjectUtils.asMap(
                    "public_id",     publicId,
                    "overwrite",     true,
                    "resource_type", "image",
                    "format",        "jpg"
            ));

            log.info("Uploaded {} cover to Cloudinary: {}", sizeName, publicId);
        }
        // Trả về base path để lưu vào DB — tương tự như "datePath/bookId" trước đây
        return COVER_FOLDER + "/" + bookId;
    }

    /**
     * Xóa tất cả 3 ảnh bìa khỏi Cloudinary khi xóa sách.
     *
     * @param coverPath Giá trị coverPath lưu trong DB
     */
    public void deleteCoverImages(String coverPath) {
        if (coverPath == null || coverPath.isBlank()) return;

        COVER_SIZES.keySet().forEach(size -> {
            String publicId = coverPath + "-" + size;
            try {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Deleted from Cloudinary: {}", publicId);
            } catch (IOException e) {
                log.warn("Không thể xóa ảnh Cloudinary {}: {}", publicId, e.getMessage());
            }
        });
    }

    /**
     * Lấy URL ảnh bìa theo size từ Cloudinary.
     *
     * @param coverPath Giá trị coverPath từ DB
     * @param size      "thumbnail" | "medium" | "large"
     * @return URL ảnh công khai trên Cloudinary
     */
    public String getCoverUrl(String coverPath, String size) {
        String resolvedSize = COVER_SIZES.containsKey(size) ? size : "medium";
        String publicId = coverPath + "-" + resolvedSize;
        return cloudinary.url().generate(publicId) + ".jpg";
    }

    // ─────────────────────────────────────────────────────────────
    // Avatar người dùng
    // ─────────────────────────────────────────────────────────────

    /**
     * Upload avatar người dùng lên Cloudinary (1 ảnh 300x300).
     *
     * @param originalImage BufferedImage gốc
     * @param userId        ID người dùng
     * @return URL avatar công khai
     */
    public String uploadAvatar(BufferedImage originalImage, String userId) throws IOException {
        String publicId   = AVATAR_FOLDER + "/" + userId;
        byte[] resizedBytes = resizeToBytes(originalImage, 300);

        Map<?, ?> result = cloudinary.uploader().upload(resizedBytes, ObjectUtils.asMap(
                "public_id",     publicId,
                "overwrite",     true,
                "resource_type", "image",
                "format",        "jpg"
        ));

        String url = (String) result.get("secure_url");
        log.info("Uploaded avatar to Cloudinary for userId={}: {}", userId, url);
        return url;
    }

    /**
     * Xóa avatar khỏi Cloudinary khi user xóa tài khoản (nếu cần).
     */
    public void deleteAvatar(String userId) {
        String publicId = AVATAR_FOLDER + "/" + userId;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Deleted avatar from Cloudinary for userId={}", userId);
        } catch (IOException e) {
            log.warn("Không thể xóa avatar Cloudinary userId={}: {}", userId, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────

    /** Resize BufferedImage về chiều rộng targetWidth, giữ tỉ lệ, xuất ra byte[] JPEG */
    private byte[] resizeToBytes(BufferedImage image, int targetWidth) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(image)
                .width(targetWidth)
                .outputFormat("jpg")
                .outputQuality(0.85)
                .toOutputStream(out);
        return out.toByteArray();
    }
}
