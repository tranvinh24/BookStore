package com.example.BookVerse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookImportResult {
    int totalRows;
    int successCount;
    int failedCount;

    @Builder.Default
    List<BookRespone> importedBooks = new ArrayList<>();

    @Builder.Default
    List<RowErrorDetail> errors = new ArrayList<>();
}
