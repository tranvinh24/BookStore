package com.example.BookVerse.service;

import com.example.BookVerse.dto.response.RowErrorDetail;
import com.example.BookVerse.entity.Book;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.*;

@Slf4j
@Service
public class BookFileParserService {

    @Getter
    @AllArgsConstructor
    public static class ParsedBookItem {
        private final Book book;
        private final String imageUrl;
    }

    @Getter
    public static class ParseResult {
        private final List<ParsedBookItem> validItems = new ArrayList<>();
        private final List<RowErrorDetail> errors = new ArrayList<>();
        private int totalRows = 0;

        public List<Book> getValidBooks() {
            return validItems.stream().map(ParsedBookItem::getBook).toList();
        }
    }

    /**
     * Phân tích tập tin (Excel hoặc CSV) thành danh sách Book và danh sách lỗi từng dòng.
     */
    public ParseResult parseFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Tên file không hợp lệ");
        }

        String lower = filename.toLowerCase();
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            return parseExcel(file.getInputStream());
        } else if (lower.endsWith(".csv")) {
            return parseCsv(file.getInputStream());
        } else {
            throw new IllegalArgumentException("Định dạng file không được hỗ trợ. Vui lòng sử dụng .xlsx, .xls hoặc .csv");
        }
    }

    /**
     * Phân tích file Excel (.xlsx / .xls)
     */
    public ParseResult parseExcel(InputStream inputStream) throws IOException {
        ParseResult result = new ParseResult();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return result;
            }

            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                return result;
            }

            // Đọc Header
            Row headerRow = rowIterator.next();
            Map<String, Integer> headerMap = mapExcelHeaders(headerRow);

            int rowIndex = 1;
            while (rowIterator.hasNext()) {
                rowIndex++;
                Row row = rowIterator.next();
                if (isExcelRowEmpty(row)) {
                    continue;
                }

                result.totalRows++;
                try {
                    ParsedBookItem item = parseExcelRow(row, headerMap, rowIndex);
                    result.getValidItems().add(item);
                } catch (Exception e) {
                    String title = getExcelString(row, headerMap.get("title"));
                    result.getErrors().add(new RowErrorDetail(rowIndex, title != null ? title : "(Dòng " + rowIndex + ")", e.getMessage()));
                }
            }
        }

        return result;
    }

    /**
     * Phân tích file CSV (hỗ trợ UTF-8 & BOM)
     */
    public ParseResult parseCsv(InputStream inputStream) throws IOException {
        ParseResult result = new ParseResult();

        // Xử lý loại bỏ BOM nếu có
        PushbackInputStream pushbackStream = new PushbackInputStream(inputStream, 3);
        byte[] bom = new byte[3];
        int n = pushbackStream.read(bom, 0, bom.length);
        if (n == 3 && bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
            // Đã là BOM UTF-8, không cần đẩy lại
        } else if (n > 0) {
            pushbackStream.unread(bom, 0, n);
        }

        try (Reader reader = new BufferedReader(new InputStreamReader(pushbackStream, StandardCharsets.UTF_8))) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .setIgnoreEmptyLines(true)
                    .build();

            try (CSVParser csvParser = new CSVParser(reader, format)) {
                Map<String, Integer> headerMap = mapCsvHeaders(csvParser.getHeaderMap());

                int rowIndex = 1; // dòng 1 là header
                for (CSVRecord record : csvParser) {
                    rowIndex++;
                    if (isCsvRecordEmpty(record)) {
                        continue;
                    }

                    result.totalRows++;
                    try {
                        ParsedBookItem item = parseCsvRecord(record, headerMap, rowIndex);
                        result.getValidItems().add(item);
                    } catch (Exception e) {
                        String title = getCsvValue(record, headerMap.get("title"));
                        result.getErrors().add(new RowErrorDetail(rowIndex, title != null ? title : "(Dòng " + rowIndex + ")", e.getMessage()));
                    }
                }
            }
        }

        return result;
    }

    private Map<String, Integer> mapExcelHeaders(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (Cell cell : headerRow) {
            String val = getCellValueAsString(cell).trim().toLowerCase();
            normalizeHeader(val, cell.getColumnIndex(), map);
        }
        return map;
    }

    private Map<String, Integer> mapCsvHeaders(Map<String, Integer> rawHeaderMap) {
        Map<String, Integer> map = new HashMap<>();
        if (rawHeaderMap == null) return map;

        for (Map.Entry<String, Integer> entry : rawHeaderMap.entrySet()) {
            if (entry.getKey() != null) {
                String val = entry.getKey().trim().toLowerCase();
                normalizeHeader(val, entry.getValue(), map);
            }
        }
        return map;
    }

    private void normalizeHeader(String headerText, int colIndex, Map<String, Integer> map) {
        if (headerText.contains("title") || headerText.contains("tên") || headerText.contains("ten sach") || headerText.contains("tựa")) {
            map.putIfAbsent("title", colIndex);
        } else if (headerText.contains("author") || headerText.contains("tác giả") || headerText.contains("tac gia")) {
            map.putIfAbsent("author", colIndex);
        } else if (headerText.contains("category") || headerText.contains("thể loại") || headerText.contains("the loai") || headerText.contains("danh mục")) {
            map.putIfAbsent("category", colIndex);
        } else if (headerText.contains("price") || headerText.contains("giá") || headerText.contains("gia ban") || headerText.contains("đơn giá")) {
            map.putIfAbsent("price", colIndex);
        } else if (headerText.contains("stock") || headerText.contains("kho") || headerText.contains("tồn") || headerText.contains("số lượng") || headerText.contains("so luong")) {
            map.putIfAbsent("stock", colIndex);
        } else if (headerText.contains("year") || headerText.contains("năm") || headerText.contains("nam xb") || headerText.contains("xuất bản")) {
            map.putIfAbsent("year", colIndex);
        } else if (headerText.contains("isbn")) {
            map.putIfAbsent("isbn", colIndex);
        } else if (headerText.contains("rating") || headerText.contains("đánh giá") || headerText.contains("danh gia") || headerText.contains("sao")) {
            map.putIfAbsent("rating", colIndex);
        } else if (headerText.contains("image") || headerText.contains("cover") || headerText.contains("ảnh") || headerText.contains("anh") || headerText.contains("hinh") || headerText.contains("url")) {
            map.putIfAbsent("imageUrl", colIndex);
        } else if (headerText.contains("description") || headerText.contains("mô tả") || headerText.contains("mo ta") || headerText.contains("tóm tắt")) {
            map.putIfAbsent("description", colIndex);
        }
    }

    private ParsedBookItem parseExcelRow(Row row, Map<String, Integer> headerMap, int rowIndex) {
        String title = getExcelString(row, headerMap.get("title"));
        String author = getExcelString(row, headerMap.get("author"));
        String category = getExcelString(row, headerMap.get("category"));
        Long price = getExcelLong(row, headerMap.get("price"));
        Integer stock = getExcelInt(row, headerMap.get("stock"));
        Integer year = getExcelInt(row, headerMap.get("year"));
        String isbn = getExcelString(row, headerMap.get("isbn"));
        Double rating = getExcelDouble(row, headerMap.get("rating"));
        String imageUrl = getExcelString(row, headerMap.get("imageUrl"));
        String description = getExcelString(row, headerMap.get("description"));

        Book book = validateAndBuildBook(title, author, category, price, stock, year, isbn, rating, description, rowIndex);
        return new ParsedBookItem(book, imageUrl);
    }

    private ParsedBookItem parseCsvRecord(CSVRecord record, Map<String, Integer> headerMap, int rowIndex) {
        String title = getCsvValue(record, headerMap.get("title"));
        String author = getCsvValue(record, headerMap.get("author"));
        String category = getCsvValue(record, headerMap.get("category"));
        Long price = parseLongSafe(getCsvValue(record, headerMap.get("price")));
        Integer stock = parseIntSafe(getCsvValue(record, headerMap.get("stock")));
        Integer year = parseIntSafe(getCsvValue(record, headerMap.get("year")));
        String isbn = getCsvValue(record, headerMap.get("isbn"));
        Double rating = parseDoubleSafe(getCsvValue(record, headerMap.get("rating")));
        String imageUrl = getCsvValue(record, headerMap.get("imageUrl"));
        String description = getCsvValue(record, headerMap.get("description"));

        Book book = validateAndBuildBook(title, author, category, price, stock, year, isbn, rating, description, rowIndex);
        return new ParsedBookItem(book, imageUrl);
    }

    private Book validateAndBuildBook(
            String title, String author, String category,
            Long price, Integer stock, Integer year,
            String isbn, Double rating, String description,
            int rowIndex) {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Tên sách không được để trống");
        }
        if (title.length() > 200) {
            throw new IllegalArgumentException("Tên sách không được vượt quá 200 ký tự");
        }

        if (author == null || author.isBlank()) {
            throw new IllegalArgumentException("Tác giả không được để trống");
        }
        if (author.length() > 200) {
            throw new IllegalArgumentException("Tên tác giả không được vượt quá 200 ký tự");
        }

        if (category == null || category.isBlank()) {
            category = "Tổng hợp";
        }

        if (price == null) {
            throw new IllegalArgumentException("Giá bán không hợp lệ hoặc để trống");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Giá bán phải >= 0");
        }

        if (stock == null) {
            stock = 0;
        }
        if (stock < 0) {
            throw new IllegalArgumentException("Số lượng tồn kho phải >= 0");
        }

        int currentYear = Year.now().getValue();
        if (year == null) {
            year = currentYear;
        } else if (year < 1900 || year > 2100) {
            throw new IllegalArgumentException("Năm xuất bản phải nằm trong khoảng 1900 - 2100 (nhận được: " + year + ")");
        }

        if (isbn != null && !isbn.isBlank()) {
            isbn = isbn.trim();
            if (isbn.length() > 25) {
                throw new IllegalArgumentException("ISBN không được vượt quá 25 ký tự");
            }
        } else {
            isbn = null;
        }

        if (rating == null) {
            rating = 5.0;
        } else if (rating < 0.0 || rating > 5.0) {
            throw new IllegalArgumentException("Đánh giá phải từ 0.0 đến 5.0 (nhận được: " + rating + ")");
        }

        if (description != null && description.length() > 2000) {
            description = description.substring(0, 2000);
        }

        Book book = new Book();
        book.setTitle(title.trim());
        book.setAuthor(author.trim());
        book.setCategory(category.trim());
        book.setPrice(price);
        book.setStock(stock);
        book.setYear(year);
        book.setIsbn(isbn);
        book.setRating(rating);
        book.setDescription(description != null ? description.trim() : null);

        return book;
    }

    private boolean isExcelRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellValueAsString(cell).trim();
                if (!val.isEmpty()) return false;
            }
        }
        return true;
    }

    private boolean isCsvRecordEmpty(CSVRecord record) {
        for (String val : record) {
            if (val != null && !val.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getExcelString(Row row, Integer colIndex) {
        if (colIndex == null || colIndex < 0) return null;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        String val = getCellValueAsString(cell).trim();
        return val.isEmpty() ? null : val;
    }

    private Long getExcelLong(Row row, Integer colIndex) {
        if (colIndex == null || colIndex < 0) return null;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (long) cell.getNumericCellValue();
        }
        return parseLongSafe(getCellValueAsString(cell));
    }

    private Integer getExcelInt(Row row, Integer colIndex) {
        if (colIndex == null || colIndex < 0) return null;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) Math.round(cell.getNumericCellValue());
        }
        return parseIntSafe(getCellValueAsString(cell));
    }

    private Double getExcelDouble(Row row, Integer colIndex) {
        if (colIndex == null || colIndex < 0) return null;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        return parseDoubleSafe(getCellValueAsString(cell));
    }

    private String getCsvValue(CSVRecord record, Integer colIndex) {
        if (colIndex == null || colIndex < 0 || colIndex >= record.size()) return null;
        String val = record.get(colIndex);
        if (val == null) return null;
        val = val.trim();
        return val.isEmpty() ? null : val;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num) && !Double.isInfinite(num)) {
                    return String.valueOf((long) num);
                }
                return String.valueOf(num);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    private Long parseLongSafe(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            String cleaned = str.replaceAll("[^0-9.-]", "");
            if (cleaned.isEmpty()) return null;
            if (cleaned.contains(".")) {
                return (long) Double.parseDouble(cleaned);
            }
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseIntSafe(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            String cleaned = str.replaceAll("[^0-9.-]", "");
            if (cleaned.isEmpty()) return null;
            if (cleaned.contains(".")) {
                return (int) Math.round(Double.parseDouble(cleaned));
            }
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDoubleSafe(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            String cleaned = str.replaceAll("[^0-9.-]", "");
            if (cleaned.isEmpty()) return null;
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Tạo file mẫu Excel (.xlsx) với định dạng và dữ liệu mẫu chuẩn (bao gồm Link ảnh bìa)
     */
    public byte[] generateExcelTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Danh_sach_sach");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Tên sách (*)", "Tác giả (*)", "Thể loại (*)", "Giá bán (*)",
                    "Số lượng kho (*)", "Năm XB", "ISBN-13", "Đánh giá (0-5)", "Link ảnh bìa (URL)", "Mô tả"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample Data Rows
            Object[][] sampleData = {
                    {"Clean Code", "Robert C. Martin", "Công nghệ", 250000, 50, 2020, "978-0-13-235088-4", 4.9, "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=500", "Cẩm nang lập trình sạch và chuẩn mực"},
                    {"Đắc Nhân Tâm", "Dale Carnegie", "Kỹ năng sống", 120000, 100, 2022, "978-6-04-589234-1", 4.8, "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500", "Nghệ thuật thu phục lòng người và đối nhân xử thế"},
                    {"Nhà Giả Kim", "Paulo Coelho", "Văn học", 89000, 80, 2021, "978-6-04-563212-0", 4.7, "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500", "Hành trình theo đuổi ước mơ của chàng chăn cừu Santiago"}
            };

            for (int r = 0; r < sampleData.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < sampleData[r].length; c++) {
                    Cell cell = row.createCell(c);
                    Object val = sampleData[r][c];
                    if (val instanceof Number) {
                        cell.setCellValue(((Number) val).doubleValue());
                    } else {
                        cell.setCellValue(val != null ? val.toString() : "");
                    }
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 4000));
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Tạo file mẫu CSV (.csv) kèm UTF-8 BOM và cột link ảnh
     */
    public byte[] generateCsvTemplate() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Ghi UTF-8 BOM để Excel hiển thị đúng tiếng Việt có dấu
        out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(
                     "title", "author", "category", "price", "stock", "year", "isbn", "rating", "image_url", "description"
             ).build())) {

            csvPrinter.printRecord("Clean Code", "Robert C. Martin", "Công nghệ", 250000, 50, 2020, "978-0-13-235088-4", 4.9, "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=500", "Cẩm nang lập trình sạch và chuẩn mực");
            csvPrinter.printRecord("Đắc Nhân Tâm", "Dale Carnegie", "Kỹ năng sống", 120000, 100, 2022, "978-6-04-589234-1", 4.8, "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500", "Nghệ thuật thu phục lòng người");
            csvPrinter.printRecord("Nhà Giả Kim", "Paulo Coelho", "Văn học", 89000, 80, 2021, "978-6-04-563212-0", 4.7, "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500", "Hành trình theo đuổi ước mơ");

            csvPrinter.flush();
        }

        return out.toByteArray();
    }
}
