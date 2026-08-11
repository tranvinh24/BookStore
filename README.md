# BookVerse

BookVerse là một hệ thống quản lý sách (RESTful API) được xây dựng bằng **Spring Boot 3** và **Java 21**. Dự án cung cấp các tính năng quản lý thông tin sách, bao gồm việc tải lên và xử lý ảnh bìa (hỗ trợ tự động thay đổi kích thước ảnh - resize).

## Công nghệ sử dụng

- **Ngôn ngữ:** Java 21
- **Framework chính:** Spring Boot 3.x (WebMVC, Data JPA, Validation)
- **Cơ sở dữ liệu:** H2 (In-memory cho môi trường dev) & MySQL
- **Công cụ hỗ trợ:** 
  - [Lombok](https://projectlombok.org/) (Giảm boilerplate code)
  - [MapStruct](https://mapstruct.org/) (Tự động ánh xạ Entity và DTO)
- **Xử lý ảnh:** [Thumbnailator](https://github.com/coobird/thumbnailator) (Resize ảnh JPEG/PNG/WebP hiệu quả)
- **Build tool:** Maven

## Tính năng nổi bật

- **Quản lý thông tin sách (CRUD):** Tên sách, tác giả, mã ISBN, năm xuất bản, thể loại, điểm đánh giá và mô tả chi tiết.
- **Quản lý ảnh bìa:** 
  - Hỗ trợ tải lên ảnh bìa sách (Multipart file) và lưu trữ cục bộ một cách dễ dàng.
  - Tự động lấy ảnh bìa theo kích thước yêu cầu (`thumbnail` 200px, `medium` 500px, `large` 1200px) nhằm tối ưu hiệu năng.
- **Tìm kiếm & Phân trang:** 
  - Tìm kiếm full-text theo tên sách hoặc tác giả.
  - Phân trang, sắp xếp và lọc danh sách sách (theo thể loại, năm xuất bản).

## Cấu trúc thư mục

Thư mục lưu trữ ảnh tải lên được cấu hình trong `application.yaml`:
- **Thư mục mặc định:** `uploads/covers`

## 🔌 API Endpoints

### 1. Sách (Books)

| Method | Endpoint | Mô tả |
| ------ | -------- | ----- |
| `GET` | `/api/books` | Lấy danh sách các cuốn sách (hỗ trợ phân trang `page`, `size`, filter `category`, `year`, sắp xếp `sort`, `dir`) |
| `GET` | `/api/books/search` | Tìm kiếm sách (hỗ trợ từ khóa `q`, filter `category`, phân trang và sắp xếp) |
| `GET` | `/api/books/{id}` | Xem chi tiết thông tin một cuốn sách theo ID |
| `POST` | `/api/books` | Thêm sách mới. Hỗ trợ JSON (không ảnh) hoặc `multipart/form-data` (có ảnh: `data` & `cover`) |
| `PUT` | `/api/books/{id}` | Cập nhật sách. Hỗ trợ JSON hoặc `multipart/form-data` để cập nhật cả ảnh bìa |
| `DELETE` | `/api/books/{id}` | Xóa sách và tất cả các file ảnh bìa đi kèm |

### 2. Ảnh bìa (Covers)

| Method | Endpoint | Mô tả |
| ------ | -------- | ----- |
| `GET` | `/api/books/{id}/cover` | Trả về file ảnh bìa (Thêm param `size=thumbnail|medium|large`) |

## Hướng dẫn cài đặt và chạy dự án

### Yêu cầu hệ thống:
- JDK 21+
- Maven (Dự án đã tích hợp sẵn Maven Wrapper `mvnw`)
- Cơ sở dữ liệu MySQL (Nếu không muốn dùng H2)

### Các bước thực hiện:

1. **Clone dự án về máy:**
   ```bash
   git clone <repository_url>
   cd BookVerse
   ```

2. **Cấu hình Cơ sở dữ liệu:**
   - Theo mặc định, ứng dụng dùng biến môi trường để kết nối MySQL: `jdbc:mysql://localhost:3306/bookstore`.
   - Bạn có thể tùy chỉnh thông qua việc cài đặt các biến môi trường: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
   - Hoặc chỉnh sửa trực tiếp file `src/main/resources/application.yaml`.

3. **Chạy ứng dụng bằng Maven Wrapper:**
   
   Bạn hãy cd vào thư mục backend `BookVerse` (chứa file `pom.xml`):
   ```bash
   cd BookVerse
   ./mvnw spring-boot:run
   ```
   
   Hoặc build ứng dụng thành file `.jar` và chạy:
   ```bash
   ./mvnw clean package
   java -jar target/BookVerse-0.0.1-SNAPSHOT.jar
   ```

4. **Kiểm tra ứng dụng:**
   - Ứng dụng sẽ khởi chạy tại cổng mặc định: `http://localhost:8080`.

