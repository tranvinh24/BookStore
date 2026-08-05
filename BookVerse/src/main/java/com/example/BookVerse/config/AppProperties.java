package com.example.BookVerse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Đọc các cấu hình từ application.yaml với prefix "app.upload".
 * Ví dụ: app.upload.dir, app.upload.allowed-types
 */
@Component
@ConfigurationProperties(prefix = "app.upload")
@Data
public class AppProperties {

    /** Thư mục gốc lưu ảnh, ví dụ: uploads/covers */
    private String dir = "uploads/covers";

    /** Danh sách MIME type được phép upload, ngăn cách bằng dấu phẩy */
    private String allowedTypes = "image/jpeg,image/png,image/webp";

    /**
     * Trả về danh sách các MIME type hợp lệ dưới dạng List.
     */
    public List<String> getAllowedTypeList() {
        return Arrays.asList(allowedTypes.split(","));
    }
}
