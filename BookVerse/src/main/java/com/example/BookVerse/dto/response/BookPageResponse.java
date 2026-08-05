package com.example.BookVerse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper DTO cho kết quả phân trang.
 * Trả về cùng với metadata phân trang để client dễ xử lý.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookPageResponse {

    /** Danh sách sách trong trang hiện tại */
    private List<BookRespone> content;

    /** Trang hiện tại (bắt đầu từ 0) */
    private int page;

    /** Số phần tử mỗi trang */
    private int size;

    /** Tổng số phần tử trong DB (theo filter) */
    private long totalElements;

    /** Tổng số trang */
    private int totalPages;

    /** Đây có phải trang cuối không */
    private boolean last;
}
