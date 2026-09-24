# BookVerse — Nền Tảng Thương Mại Điện Tử Và Quản Lý Sách Trực Tuyến

BookVerse là hệ thống web bán sách và quản trị thương mại điện tử hoàn chỉnh, được xây dựng trên nền tảng Spring Boot (Java 17) và kiến trúc RESTful API, kết hợp giao diện Web hiện đại, chuẩn SEO và tối ưu trải nghiệm hiển thị trên cả máy tính lẫn thiết bị di động.

---

## Mục Lục
1. [Công Nghệ Sử Dụng](#công-nghệ-sử-dụng)
2. [Tính Năng Nổi Bật](#tính-năng-nổi-bật)
3. [Kiến Trúc Và Cấu Trúc Dự Án](#kiến-trúc-và-cấu-trúc-dự-án)
4. [Tài Liệu RESTful API](#tài-liệu-restful-api)
5. [Cấu Hình Và Biến Môi Trường](#cấu-hình-và-biến-môi-trường)
6. [Hướng Dẫn Cài Đặt Và Khởi Chạy](#hướng-dẫn-cài-đặt-và-khởi-chạy)
7. [Tài Liệu Swagger / OpenAPI](#tài-liệu-swagger--openapi)
8. [Triển Khai Lên Điện Toán Đám Mây](#triển-khai-lên-điện-toán-đám-mây)

---

## Công Nghệ Sử Dụng

### Backend
- **Ngôn ngữ:** Java 17 (LTS)
- **Framework:** Spring Boot (Spring MVC, Spring Data JPA, Spring Security, Spring Validation, Spring Cache)
- **Bảo mật:** Spring Security kết hợp JSON Web Token (JJWT 0.12.6) xác thực không trạng thái (stateless), mã hóa mật khẩu BCrypt
- **Cơ sở dữ liệu:** MySQL 8.x (Hỗ trợ H2 In-Memory trong môi trường phát triển)
- **Bộ nhớ đệm (Cache):** Caffeine Cache (In-memory, tối ưu tốc độ truy xuất dữ liệu không cần thiết lập Redis server)
- **Lưu trữ đám mây:** Cloudinary CDN (Tải lên, tối ưu và phân phối ảnh bìa sách cùng ảnh đại diện người dùng)
- **Xử lý hình ảnh:** Thumbnailator (Tự động thay đổi kích thước ảnh bìa: Thumbnail 200px, Medium 500px, Large 1200px)
- **Xử lý tệp tin:** Apache POI (Nhập dữ liệu sách từ tệp Excel .xlsx) và Apache Commons CSV (Nhập sách từ tệp .csv)
- **Tài liệu API:** Springdoc OpenAPI 3.0 / Swagger UI (springdoc-openapi-starter-webmvc-ui 2.8.5)
- **Đóng gói và Triển khai:** Maven Wrapper, Docker (Multi-stage build)

### Frontend
- **Giao diện:** HTML5, CSS3, Vanilla JavaScript (Kiến trúc Single Page Components)
- **Thiết kế:** Tông màu Đỏ thẫm kết hợp Cam đất chuyên nghiệp, phông chữ Be Vietnam Pro và Space Grotesk
- **Tương thích thiết bị (Responsive):** Tối ưu giao diện cho Desktop, Tablet và Màn hình điện thoại thông minh (hỗ trợ thao tác vuốt chạm, bố cục lưới 2 cột chuẩn thương mại điện tử di động)

---

## Tính Năng Nổi Bật

### 1. Phân Hệ Người Dùng (Khách hàng)
- **Khám phá sách:** Danh mục sách đa dạng, bộ lọc nâng cao (thể loại, khoảng giá, năm phát hành), tìm kiếm toàn văn (full-text search), xem chi tiết sách cùng ảnh bìa độ phân giải cao.
- **Đánh giá và Bình luận:** Chấm điểm theo thang 1 - 5 sao và để lại nhận xét cho từng cuốn sách.
- **Giỏ hàng và Danh sách yêu thích:** Quản lý sản phẩm chọn mua, cập nhật số lượng, lưu trữ danh sách yêu thích theo từng tài khoản.
- **Đặt hàng và Thanh toán:**
  - Thanh toán khi nhận hàng (COD).
  - Thanh toán trực tuyến qua Cổng VNPay Sandbox (tạo chữ ký bảo mật HMAC-SHA512, tự động xác thực kết quả giao dịch).
- **Quản lý đơn mua:** Theo dõi lịch sử đặt hàng, mã vận đơn, trạng thái tiến trình đơn hàng (Chờ thanh toán, Đang xử lý, Đang giao, Đã giao, Đã hủy).
- **Hồ sơ cá nhân:** Cập nhật thông tin liên hệ, đổi mật khẩu, tải lên ảnh đại diện lưu trữ trên Cloudinary.
- **Hệ thống thông báo:** Nhận thông báo tự động trên thanh điều hướng khi trạng thái đơn hàng thay đổi.

### 2. Phân Hệ Quản Trị Viên (Admin)
- **Bảng điều khiển (Dashboard):** Thống kê doanh thu theo ngày, theo tháng, theo dõi số lượng đơn hàng và người dùng mới.
- **Quản lý danh mục sách:** Thêm, sửa, xóa sách; cập nhật tồn kho; cảnh báo sách sắp hết hàng; nhập sách hàng loạt qua tệp Excel/CSV.
- **Quản lý đơn hàng:** Xem toàn bộ đơn hàng trong hệ thống, cập nhật trạng thái đơn (Duyệt đơn, Đang giao, Đã giao, Hủy đơn), lưu thông tin vận đơn.
- **Quản lý người dùng:** Tra cứu danh sách người dùng, kích hoạt hoặc khóa tài khoản, phân quyền quản trị viên.

---

## Kiến Trúc Và Cấu Trúc Dự Án

```
BookVerse/
├── BookVerse/                     # Thư mục mã nguồn chính (Spring Boot)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/BookVerse/
│   │   │   │   ├── config/        # Cấu hình Security, Cache, Cloudinary, VNPay, OpenAPI, WebMvc
│   │   │   │   ├── controller/    # REST Controllers
│   │   │   │   ├── dto/           # Data Transfer Objects (Request và Response)
│   │   │   │   ├── entity/        # JPA Entities
│   │   │   │   ├── exception/     # Xử lý ngoại lệ tập trung (GlobalExceptionHandler)
│   │   │   │   ├── Mapper/        # Spring Component Mappers
│   │   │   │   ├── repository/    # Spring Data JPA Repositories
│   │   │   │   └── service/       # Business Logic Services
│   │   │   └── resources/
│   │   │       ├── application.yaml # Cấu hình ứng dụng
│   │   │       └── static/        # Giao diện Web (HTML, CSS, JS, Images)
│   │   └── test/                  # Kiểm thử tự động
│   ├── Dockerfile                 # Dockerfile build nội bộ
│   └── pom.xml                    # Quản lý thư viện Maven
├── Dockerfile                     # Dockerfile triển khai gốc cho nền tảng Cloud
├── README.md                      # Tài liệu dự án
└── AGENTS.md                      # Quy chuẩn phát triển dự án
```

---

## Tài Liệu RESTful API

### 1. Xác Thực (Authentication)
| Phương thức | Endpoint | Quyền hạn | Mô tả |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Public | Đăng ký tài khoản mới |
| `POST` | `/api/auth/login` | Public | Đăng nhập nhận JWT Token |

### 2. Quản Lý Sách (Books)
| Phương thức | Endpoint | Quyền hạn | Mô tả |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/books` | Public | Danh sách sách (phân trang, sắp xếp, lọc) |
| `GET` | `/api/books/{id}` | Public | Thông tin chi tiết một cuốn sách |
| `GET` | `/api/books/search` | Public | Tìm kiếm sách theo từ khóa |
| `GET` | `/api/books/{id}/cover` | Public | Lấy URL ảnh bìa (size: thumbnail, medium, large) |
| `POST` | `/api/books` | ADMIN | Thêm sách mới (JSON hoặc Multipart kèm ảnh) |
| `PUT` | `/api/books/{id}` | ADMIN | Cập nhật thông tin hoặc ảnh bìa |
| `DELETE` | `/api/books/{id}` | ADMIN | Xóa sách và tài nguyên ảnh trên Cloud |

### 3. Đánh Giá Sách (Reviews)
| Phương thức | Endpoint | Quyền hạn | Mô tả |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/books/{id}/reviews` | Public | Danh sách đánh giá của sách |
| `POST` | `/api/books/{id}/reviews` | USER/ADMIN | Gửi bình luận và chấm điểm |
| `DELETE` | `/api/reviews/{id}` | USER/ADMIN | Xóa đánh giá |

### 4. Giỏ Hàng Và Yêu Thích (Cart & Wishlist)
| Phương thức | Endpoint | Quyền hạn | Mô tả |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/cart` | USER | Lấy thông tin giỏ hàng hiện tại |
| `POST` | `/api/cart/items` | USER | Thêm sản phẩm vào giỏ hàng |
| `PUT` | `/api/cart/items/{id}` | USER | Cập nhật số lượng sản phẩm |
| `DELETE` | `/api/cart/items/{id}` | USER | Xóa sản phẩm khỏi giỏ hàng |
| `DELETE` | `/api/cart/clear` | USER | Xóa toàn bộ giỏ hàng |
| `GET` | `/api/wishlist` | USER | Danh sách sách yêu thích |
| `POST` | `/api/wishlist/{bookId}`| USER | Thêm vào danh sách yêu thích |
| `DELETE` | `/api/wishlist/{bookId}`| USER | Xóa khỏi danh sách yêu thích |

### 5. Đơn Hàng Và Thanh Toán (Orders & Payments)
| Phương thức | Endpoint | Quyền hạn | Mô tả |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/orders/checkout` | USER | Tạo đơn hàng từ giỏ hàng |
| `GET` | `/api/orders/my-orders` | USER | Danh sách đơn hàng đã mua |
| `GET` | `/api/orders/{id}` | USER | Thông tin chi tiết một đơn hàng |
| `POST` | `/api/payments/{orderId}/vnpay-url` | USER | Khởi tạo liên kết thanh toán VNPay |
| `GET` | `/api/payments/vnpay-callback` | Public | Tiếp nhận và xác thực kết quả từ VNPay |

### 6. Hồ Sơ Cá Nhân (Profile)
| Phương thức | Endpoint | Quyền hạn | Mô tả |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/profile` | Authenticated | Thông tin tài khoản đăng nhập |
| `PUT` | `/api/profile` | Authenticated | Cập nhật thông tin cá nhân |
| `POST` | `/api/profile/avatar` | Authenticated | Tải lên ảnh đại diện |
| `PUT` | `/api/profile/password` | Authenticated | Thay đổi mật khẩu đăng nhập |

### 7. Phân Hệ Quản Trị (Admin)
| Phương thức | Endpoint | Quyền hạn | Mô tả |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/dashboard` | ADMIN | Số liệu thống kê tổng quan |
| `GET` | `/api/admin/revenue/daily` | ADMIN | Doanh thu theo từng ngày |
| `GET` | `/api/admin/revenue/monthly`| ADMIN | Doanh thu theo từng tháng |
| `GET` | `/api/admin/orders` | ADMIN | Quản lý toàn bộ đơn hàng |
| `PUT` | `/api/admin/orders/{id}/status` | ADMIN | Cập nhật trạng thái đơn và vận đơn |
| `GET` | `/api/admin/users` | ADMIN | Quản lý tài khoản người dùng |
| `PUT` | `/api/admin/books/{id}/stock` | ADMIN | Điều chỉnh tồn kho sản phẩm |
| `POST` | `/api/admin/books/import` | ADMIN | Nhập dữ liệu sách từ tệp Excel/CSV |

---

## Cấu Hình Và Biến Môi Trường

Hệ thống hỗ trợ cấu hình qua tệp `application.yaml` hoặc ghi đè thông qua biến môi trường của hệ điều hành / dịch vụ triển khai:

| Tên Biến | Mô Tả | Giá Trị Mẫu / Mặc Định |
| :--- | :--- | :--- |
| `DB_URL` | Chuỗi kết nối JDBC MySQL | `jdbc:mysql://localhost:3306/bookstore` |
| `DB_USERNAME` | Tên người dùng cơ sở dữ liệu | `root` |
| `DB_PASSWORD` | Mật khẩu cơ sở dữ liệu | *(Thiết lập theo môi trường)* |
| `JWT_SECRET` | Khóa bí mật ký JWT Token (tối thiểu 64 ký tự) | *(Chuỗi ngẫu nhiên bảo mật)* |
| `CLOUDINARY_CLOUD_NAME` | Tên Cloud Name trên Cloudinary | *(Thiết lập theo tài khoản)* |
| `CLOUDINARY_API_KEY` | API Key của tài khoản Cloudinary | *(Thiết lập theo tài khoản)* |
| `CLOUDINARY_API_SECRET` | API Secret của tài khoản Cloudinary | *(Thiết lập theo tài khoản)* |
| `VNPAY_TMN_CODE` | Mã định danh website (TMN Code) VNPay Sandbox | *(Mã do VNPay cấp)* |
| `VNPAY_HASH_SECRET` | Khóa bí mật tạo chữ ký hash VNPay Sandbox | *(Khóa do VNPay cấp)* |
| `VNPAY_RETURN_URL` | URL nhận kết quả sau thanh toán VNPay | `http://localhost:8080/payment.html` |

---

## Hướng Dẫn Cài Đặt Và Khởi Chạy

### Yêu Cầu Môi Trường
- Java Development Kit (JDK) phiên bản 17 trở lên
- Apache Maven phiên bản 3.8 trở lên (hoặc sử dụng Maven Wrapper đi kèm)
- Hệ quản trị cơ sở dữ liệu MySQL 8.0 trở lên

### Các Bước Thực Hiện:

1. **Sao chép mã nguồn:**
   ```bash
   git clone <repository-url>
   cd BookVerse
   ```

2. **Khởi tạo Cơ sở dữ liệu:**
   - Tạo cơ sở dữ liệu trong MySQL:
     ```sql
     CREATE DATABASE bookstore CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
     ```
   - Cấu hình thông tin kết nối trong `BookVerse/src/main/resources/application.yaml` hoặc thiết lập qua các biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

3. **Khởi chạy ứng dụng:**
   - Chuyển vào thư mục chứa dự án:
     ```bash
     cd BookVerse
     ```
   - Chạy trực tiếp qua Maven:
     ```bash
     mvn spring-boot:run
     ```
   - Hoặc đóng gói tệp thực thi JAR:
     ```bash
     mvn clean package -DskipTests
     java -jar target/BookVerse-0.0.1-SNAPSHOT.jar
     ```

4. **Truy cập ứng dụng:**
   - Giao diện web được phục vụ tại: `http://localhost:8080`

---

## Tài Liệu Swagger / OpenAPI

Dự án cung cấp giao diện thử nghiệm API trực tiếp thông qua **Swagger UI**:

- **Đường dẫn truy cập:**
  ```
  http://localhost:8080/swagger-ui/index.html
  ```
- **Xác thực API trên Swagger UI:**
  1. Gửi yêu cầu đăng nhập tại endpoint `POST /api/auth/login` để nhận chuỗi JWT token.
  2. Nhấn nút **Authorize** ở góc trên bên phải màn hình Swagger UI.
  3. Nhập chuỗi JWT token vào trường **Value** và nhấn **Authorize**.
  4. Thực hiện gửi yêu cầu kiểm tra các API yêu cầu quyền hạn trực tiếp trên giao diện trình duyệt.

---

## Triển Khai Lên Điện Toán Đám Mây

Dự án được chuẩn hóa để triển khai trên các nền tảng điện toán đám mây:

1. **Cơ sở dữ liệu:** Sử dụng dịch vụ MySQL trên đám mây (hỗ trợ SSL/TLS).
2. **Lưu trữ tệp tin:** Sử dụng dịch vụ lưu trữ đám mây Cloudinary, đảm bảo an toàn dữ liệu ảnh bìa và ảnh đại diện độc lập với vòng đời của máy chủ ứng dụng.
3. **Triển khai ứng dụng (Container):**
   - Sử dụng Dockerfile đa giai đoạn (multi-stage build) đi kèm trong dự án.
   - Nền tảng đích: Render Web Service (môi trường Docker), AWS, Google Cloud Run hoặc Kubernetes.
   - Cung cấp đầy đủ các biến môi trường cấu hình tại bảng điều khiển của dịch vụ triển khai trước khi kích hoạt quy trình build và khởi chạy.

---

*Hệ thống được phát triển và tối ưu hóa nhằm đáp ứng tiêu chuẩn kiến trúc hiện đại, khả năng mở rộng linh hoạt và bảo mật dữ liệu.*
