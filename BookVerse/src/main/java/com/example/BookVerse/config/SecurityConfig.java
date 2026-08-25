package com.example.BookVerse.config;

import com.example.BookVerse.service.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;

/**
 * Cấu hình Spring Security cho BookVerse.
 * Sử dụng JWT stateless — không dùng session.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter              jwtFilter;
    private final UserDetailsServiceImpl userDetailsService;

    // ──────────────────────────────────────────────
    // Phân quyền endpoint
    // ──────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Tắt CSRF vì dùng JWT stateless
            .csrf(AbstractHttpConfigurer::disable)

            // Không tạo session
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Xử lý lỗi xác thực & phân quyền trả về JSON chuẩn
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(customAuthenticationEntryPoint())
                .accessDeniedHandler(customAccessDeniedHandler())
            )

            // Phân quyền
            .authorizeHttpRequests(auth -> auth
                // Public: đăng ký / đăng nhập
                .requestMatchers("/api/auth", "/api/auth/**").permitAll()

                // Public: đọc sách và ảnh bìa
                .requestMatchers(HttpMethod.GET, "/api/books", "/api/books/**").permitAll()

                // Public: các file tĩnh (HTML/JS/CSS)
                .requestMatchers(
                    "/", "/index.html", "/login.html", "/register.html",
                    "/css/**", "/js/**", "/images/**", "/uploads/**", "/favicon.ico"
                ).permitAll()

                // Chỉ ADMIN: thêm, sửa, xóa sách
                .requestMatchers(HttpMethod.POST,   "/api/books", "/api/books/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/books", "/api/books/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/books", "/api/books/**").hasRole("ADMIN")

                // Phase 2: USER endpoints — Wishlist, Cart, Order, Payment
                .requestMatchers("/api/wishlist", "/api/wishlist/**").hasRole("USER")
                .requestMatchers("/api/cart", "/api/cart/**").hasRole("USER")
                .requestMatchers("/api/orders", "/api/orders/**").hasRole("USER")
                .requestMatchers("/api/payments", "/api/payments/**").hasRole("USER")

                // Phase 3: ADMIN endpoints
                .requestMatchers("/api/admin", "/api/admin/**").hasRole("ADMIN")

                // Phase 4: Profile — bất kỳ user đã đăng nhập
                .requestMatchers("/api/profile", "/api/profile/**").authenticated()

                // Còn lại: phải đăng nhập
                .anyRequest().authenticated()
            )

            // Thêm JwtFilter trước filter xác thực mặc định
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

            // Dùng AuthenticationProvider tùy chỉnh
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    // ──────────────────────────────────────────────
    // Custom Error Handlers for Security Filter Chain
    // ──────────────────────────────────────────────

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            String json = String.format(
                "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"code\":\"UNAUTHENTICATED\",\"message\":\"Bạn chưa đăng nhập hoặc token không hợp lệ\",\"path\":\"%s\"}",
                LocalDateTime.now(), request.getRequestURI()
            );
            response.getWriter().write(json);
        };
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            String json = String.format(
                "{\"timestamp\":\"%s\",\"status\":403,\"error\":\"Forbidden\",\"code\":\"UNAUTHORIZED\",\"message\":\"Bạn không có quyền thực hiện hành động này\",\"path\":\"%s\"}",
                LocalDateTime.now(), request.getRequestURI()
            );
            response.getWriter().write(json);
        };
    }

    // ──────────────────────────────────────────────
    // Beans xác thực
    // ──────────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
}
