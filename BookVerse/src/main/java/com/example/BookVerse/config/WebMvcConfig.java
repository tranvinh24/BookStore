package com.example.BookVerse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Cấu hình phục vụ file tĩnh từ thư mục ngoài classpath:
 * - /uploads/covers/** → thư mục vật lý uploads/covers trên disk
 * - /uploads/avatars/** → thư mục vật lý uploads/avatars trên disk
 *
 * Cách này cho phép browser load ảnh trực tiếp mà không cần JWT token.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve thư mục gốc của project (chạy từ IDE: target/classes -> target -> project)
        String projectRoot = resolveProjectRoot();

        // Serves ảnh bìa sách: GET /uploads/covers/**
        registry.addResourceHandler("/uploads/covers/**")
                .addResourceLocations("file:" + projectRoot + "/uploads/covers/");

        // Serves ảnh đại diện (avatar): GET /uploads/avatars/**
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations("file:" + projectRoot + "/uploads/avatars/");
    }

    private String resolveProjectRoot() {
        try {
            Path classLocation = Paths.get(
                WebMvcConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
            Path root = classLocation.getParent();
            if (root.endsWith("classes")) {
                root = root.getParent().getParent(); // target/classes -> target -> project
            } else {
                root = root.getParent();             // target -> project
            }
            return root.toAbsolutePath().toString();
        } catch (Exception e) {
            return System.getProperty("user.dir");
        }
    }
}
