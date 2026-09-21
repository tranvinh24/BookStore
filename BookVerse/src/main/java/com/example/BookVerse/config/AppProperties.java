package com.example.BookVerse.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Đọc các cấu hình từ application.yaml với prefix "app.upload".
 * Ví dụ: app.upload.dir, app.upload.allowed-types
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "app.upload")
@Data
public class AppProperties {

    /** Thư mục gốc lưu ảnh, ví dụ: uploads/covers */
    private String dir = "uploads/covers";

    /** Danh sách MIME type được phép upload, ngăn cách bằng dấu phẩy */
    private String allowedTypes = "image/jpeg,image/png,image/webp";

    /**
     * Trả về đường dẫn tuyệt đối tới thư mục upload.
     * Nếu dir đã là absolute thì dùng nguyên.
     * Nếu là relative, resolve từ thư mục chứa JAR (hoặc thư mục target/classes).
     * Cách này đảm bảo nhất quán dù app chạy từ IDE, mvnw hay java -jar.
     */
    public Path getAbsoluteUploadDir() {
        Path dirPath = Paths.get(dir);
        if (dirPath.isAbsolute()) {
            return dirPath;
        }
        // Lấy thư mục chứa class/JAR làm gốc rồi đi lên để tìm project root
        try {
            Path classLocation = Paths.get(
                AppProperties.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
            // classLocation có thể là:
            //   - .../target/classes/          (IDE / mvnw spring-boot:run)
            //   - .../target/BookVerse-x.jar   (java -jar)
            // Đi lên 2 cấp (classes -> target -> project root)
            Path projectRoot = classLocation.getParent();
            if (projectRoot.endsWith("classes")) {
                projectRoot = projectRoot.getParent().getParent(); // target -> project root
            } else {
                projectRoot = projectRoot.getParent();             // target -> project root
            }
            Path resolved = projectRoot.resolve(dir).normalize();
            log.info("Upload dir resolved to: {}", resolved.toAbsolutePath());
            return resolved;
        } catch (URISyntaxException e) {
            // Fallback: dùng user.dir (hành vi cũ)
            log.warn("Không resolve được upload dir từ JAR location, fallback user.dir");
            return Paths.get(System.getProperty("user.dir")).resolve(dir);
        }
    }

    /**
     * Trả về danh sách các MIME type hợp lệ dưới dạng List.
     */
    public List<String> getAllowedTypeList() {
        return Arrays.asList(allowedTypes.split(","));
    }
}
