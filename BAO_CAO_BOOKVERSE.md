# BÁO CÁO DỰ ÁN: BOOKVERSE

> **Ngày thực hiện:** 2026

---

## 1. Giới thiệu Project

### 1.1. Tổng quan

**BookVerse** là một hệ thống quản lý và mua bán sách trực tuyến được xây dựng dưới dạng **RESTful API Backend**. Hệ thống cho phép người dùng tra cứu, tìm kiếm, đặt mua sách và quản lý đơn hàng; đồng thời cung cấp bộ công cụ quản trị dành cho admin để kiểm soát sách, tài khoản và doanh thu.

### 1.2. Mục tiêu

- Xây dựng một backend API hoàn chỉnh phục vụ ứng dụng mua sách trực tuyến.
- Hỗ trợ xác thực bảo mật qua JWT và phân quyền theo vai trò (ADMIN / USER).
- Quản lý toàn bộ vòng đời đơn hàng từ khi tạo đến khi giao hàng.
- Cung cấp tính năng upload và tự động resize ảnh bìa sách.

### 1.3. Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 17 |
| Framework | Spring Boot 4.1.0 |
| Bảo mật | Spring Security + JWT (jjwt 0.12.6) |
| ORM | Spring Data JPA + Hibernate |
| Cơ sở dữ liệu | MySQL (production) / H2 in-memory (dev) |
| Ánh xạ DTO | MapStruct 1.6.3 |
| Giảm boilerplate | Lombok 1.18.x |
| Xử lý ảnh | Thumbnailator 0.4.20 |
| Build tool | Maven (Maven Wrapper) |

---

## 2. Phân tích Yêu cầu

### 2.1. Yêu cầu chức năng

#### Nhóm chức năng dành cho Người dùng (USER)
- **Đăng ký / Đăng nhập:** Tạo tài khoản mới, đăng nhập bằng username/password, nhận JWT token.
- **Quản lý hồ sơ:** Xem và cập nhật thông tin cá nhân (email, số điện thoại, mật khẩu).
- **Tra cứu sách:** Xem danh sách sách, lọc theo thể loại / năm xuất bản, sắp xếp, phân trang, tìm kiếm full-text.
- **Xem ảnh bìa:** Lấy ảnh bìa theo kích thước (thumbnail 200px / medium 500px / large 1200px).
- **Giỏ hàng:** Thêm, sửa số lượng, xóa sản phẩm, xem giỏ hàng.
- **Danh sách yêu thích (Wishlist):** Thêm / xóa sách yêu thích.
- **Đặt hàng:** Tạo đơn hàng từ giỏ hàng, nhập địa chỉ giao hàng.
- **Thanh toán:** Xác nhận thanh toán, chọn phương thức thanh toán.
- **Xem đơn hàng:** Theo dõi lịch sử và trạng thái đơn hàng.

#### Nhóm chức năng dành cho Quản trị viên (ADMIN)
- **Quản lý sách:** CRUD sách bao gồm upload ảnh bìa.
- **Quản lý đơn hàng:** Xem tất cả đơn, lọc theo trạng thái, cập nhật trạng thái, ghi tracking note.
- **Quản lý tài khoản:** Xem danh sách user, cập nhật thông tin, xóa tài khoản.
- **Quản lý tồn kho:** Cập nhật số lượng tồn kho, xem sách sắp hết hàng.
- **Thống kê doanh thu:** Doanh thu theo ngày và theo tháng.

### 2.2. Yêu cầu phi chức năng

- API trả về JSON chuẩn, mã HTTP phù hợp.
- Xác thực qua JWT, token có thời hạn 24 giờ.
- Upload file ảnh tối đa 20MB, hỗ trợ JPEG / PNG / WebP / GIF.
- Dữ liệu được validate trước khi xử lý (Bean Validation).
- Xử lý lỗi tập trung, trả về thông báo lỗi rõ ràng.

---

## 3. Kiến trúc Hệ thống

### 3.1. Mô hình kiến trúc

Hệ thống áp dụng kiến trúc **Layered Architecture (N-tier)** gồm 4 lớp chính:

```
┌──────────────────────────────────────────────┐
│             Client (HTTP Request)            │
└─────────────────────┬────────────────────────┘
                      │
┌─────────────────────▼────────────────────────┐
│         Controller Layer (REST API)          │
│  BookController, AuthController, Admin...    │
└─────────────────────┬────────────────────────┘
                      │
┌─────────────────────▼────────────────────────┐
│          Service Layer (Business Logic)      │
│  BookService, AuthService, OrderService...   │
└─────────────────────┬────────────────────────┘
                      │
┌─────────────────────▼────────────────────────┐
│         Repository Layer (Data Access)       │
│       Spring Data JPA Repositories           │
└─────────────────────┬────────────────────────┘
                      │
┌─────────────────────▼────────────────────────┐
│              Database (MySQL / H2)           │
└──────────────────────────────────────────────┘
```

### 3.2. Cấu trúc package

```
com.example.BookVerse/
├── config/           # Cấu hình Security, JWT, AppProperties
├── controller/       # Xử lý HTTP request, định nghĩa endpoint
├── service/          # Business logic
├── repository/       # Tương tác cơ sở dữ liệu (Spring Data JPA)
├── entity/           # JPA Entity ánh xạ bảng CSDL
├── dto/
│   ├── request/      # DTO nhận dữ liệu đầu vào
│   └── response/     # DTO trả về cho client
├── Mapper/           # MapStruct mapper (Entity ↔ DTO)
├── enums/            # Các kiểu liệt kê (Role, OrderStatus...)
└── exception/        # Xử lý ngoại lệ tập trung (AppException, ErrorCode)
```

### 3.3. Luồng bảo mật

```
Request → JwtFilter → SecurityConfig → Controller
              │
              └── Kiểm tra JWT token → Load UserDetails → Set SecurityContext
```

- **JwtFilter:** Đọc và xác thực Bearer token trên mỗi request.
- **SecurityConfig:** Định nghĩa public/private endpoints, phân quyền ADMIN/USER.
- **JwtUtil:** Tạo và parse JWT token, thời hạn 24 giờ.

---

## 4. Thiết kế Cơ sở Dữ liệu

### 4.1. Sơ đồ thực thể (ERD — mô tả quan hệ)

Hệ thống gồm **8 bảng chính**:

```
users ──────────── cart (1-1)
  │                  └── cart_item (1-N) ── book
  │
  └──────────────── orders (1-N)
  │                    ├── order_item (1-N) ── book
  │                    └── payment (1-1)
  │
  └──────────────── wishlist (1-N) ── book
```

### 4.2. Mô tả các bảng

#### Bảng `users`
| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| id | VARCHAR (UUID) | PK | Khóa chính |
| userName | VARCHAR(100) | UNIQUE, NOT NULL | Tên đăng nhập |
| password | VARCHAR | NOT NULL | Mật khẩu đã mã hóa (BCrypt) |
| role | ENUM | NOT NULL | USER / ADMIN |
| email | VARCHAR | UNIQUE, NOT NULL | Email |
| sdt | VARCHAR | — | Số điện thoại |
| createdAt | DATE | — | Ngày tạo |
| updatedAt | DATE | — | Ngày cập nhật |

#### Bảng `book`
| Cột | Kiểu | Mô tả |
|---|---|---|
| id | VARCHAR (UUID) | Khóa chính |
| title | VARCHAR | Tên sách |
| author | VARCHAR | Tác giả |
| isbn | VARCHAR | Mã ISBN |
| year | INTEGER | Năm xuất bản |
| category | VARCHAR | Thể loại |
| rating | DOUBLE | Điểm đánh giá |
| description | TEXT | Mô tả |
| coverPath | VARCHAR | Đường dẫn ảnh bìa |
| price | BIGINT | Giá bán (VNĐ) |
| stock | INTEGER | Số lượng tồn kho |

#### Bảng `orders`
| Cột | Kiểu | Mô tả |
|---|---|---|
| id | VARCHAR (UUID) | Khóa chính |
| user_id | VARCHAR | FK → users |
| status | ENUM | PENDING / PAID / PROCESSING / SHIPPING / DELIVERED / CANCELLED |
| totalAmount | BIGINT | Tổng tiền |
| shippingAddress | VARCHAR | Địa chỉ giao hàng |
| trackingNote | VARCHAR | Ghi chú theo dõi |
| createdAt / updatedAt | DATETIME | Timestamp |

#### Bảng `payment`
| Cột | Kiểu | Mô tả |
|---|---|---|
| id | VARCHAR (UUID) | Khóa chính |
| order_id | VARCHAR | FK → orders (UNIQUE) |
| amount | BIGINT | Số tiền |
| method | ENUM | Phương thức thanh toán |
| status | ENUM | PENDING / SUCCESS / FAILED |
| transactionId | VARCHAR | Mã giao dịch |
| paidAt | DATETIME | Thời điểm thanh toán |

#### Các bảng phụ
- **`cart`**: Giỏ hàng (1-1 với user)
- **`cart_item`**: Sản phẩm trong giỏ (nhiều-nhiều giữa cart và book)
- **`order_item`**: Sản phẩm trong đơn hàng
- **`wishlist`**: Sách yêu thích của user

---

## 5. Các Chức năng Đã Thực hiện

### 5.1. Xác thực & Phân quyền (Auth)

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/auth/register` | Đăng ký tài khoản mới |
| POST | `/api/auth/login` | Đăng nhập, nhận JWT |

- Mật khẩu mã hóa bằng BCrypt.
- JWT token thời hạn 24 giờ, chứa thông tin userId và role.
- Validate username/email trùng lặp khi đăng ký.

### 5.2. Quản lý Sách (Book)

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/books` | Danh sách sách (phân trang, lọc, sắp xếp) |
| GET | `/api/books/search` | Tìm kiếm full-text theo tên / tác giả |
| GET | `/api/books/{id}` | Chi tiết sách |
| POST | `/api/books` | Thêm sách (JSON hoặc multipart/form-data) |
| PUT | `/api/books/{id}` | Cập nhật sách |
| DELETE | `/api/books/{id}` | Xóa sách và ảnh bìa |
| GET | `/api/books/{id}/cover` | Lấy ảnh bìa (thumbnail/medium/large) |

### 5.3. Giỏ hàng (Cart)

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/cart` | Xem giỏ hàng |
| POST | `/api/cart/items` | Thêm sách vào giỏ |
| PUT | `/api/cart/items/{id}` | Cập nhật số lượng |
| DELETE | `/api/cart/items/{id}` | Xóa sản phẩm khỏi giỏ |

### 5.4. Danh sách Yêu thích (Wishlist)

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/wishlist` | Xem wishlist |
| POST | `/api/wishlist/{bookId}` | Thêm sách vào wishlist |
| DELETE | `/api/wishlist/{bookId}` | Xóa sách khỏi wishlist |

### 5.5. Đặt hàng & Thanh toán

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/orders` | Tạo đơn hàng từ giỏ |
| GET | `/api/orders` | Lịch sử đơn hàng |
| GET | `/api/orders/{id}` | Chi tiết đơn hàng |
| POST | `/api/payment/{orderId}` | Xác nhận thanh toán |

### 5.6. Hồ sơ Cá nhân (Profile)

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/profile` | Xem thông tin cá nhân |
| PUT | `/api/profile` | Cập nhật thông tin |
| PUT | `/api/profile/password` | Đổi mật khẩu |

### 5.7. Chức năng Admin

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/admin/orders` | Tất cả đơn hàng |
| PUT | `/api/admin/orders/{id}/status` | Cập nhật trạng thái đơn |
| GET | `/api/admin/users` | Tất cả tài khoản |
| PUT | `/api/admin/users/{id}` | Cập nhật tài khoản |
| DELETE | `/api/admin/users/{id}` | Xóa tài khoản |
| PUT | `/api/admin/books/{id}/stock` | Cập nhật tồn kho |
| GET | `/api/admin/books/low-stock` | Sách sắp hết hàng |
| GET | `/api/admin/revenue/monthly` | Doanh thu theo tháng |
| GET | `/api/admin/revenue/daily` | Doanh thu theo ngày |

---

## 6. Kiểm thử

### 6.1. Phương pháp kiểm thử

Dự án áp dụng kiểm thử **thủ công (manual testing)** thông qua các công cụ gửi HTTP request trực tiếp. Unit test tự động chưa được triển khai đầy đủ (chỉ có file scaffold mặc định `BookVerseApplicationTests.java`).

### 6.2. Công cụ kiểm thử

- **Postman / Thunder Client:** Gửi request thủ công đến từng endpoint.
- **H2 Console:** Kiểm tra trạng thái dữ liệu trực tiếp trong môi trường dev.
- **Spring Boot DevTools:** Hot reload trong quá trình phát triển.

### 6.3. Các trường hợp kiểm thử chính

| Chức năng | Kịch bản | Kết quả mong đợi |
|---|---|---|
| Đăng ký | Username đã tồn tại | HTTP 400, lỗi `USER_EXISTED` |
| Đăng nhập | Sai mật khẩu | HTTP 401, lỗi `INVALID_CREDENTIALS` |
| Thêm sách | Không có token | HTTP 403 Forbidden |
| Đặt hàng | Giỏ hàng rỗng | HTTP 400, thông báo lỗi phù hợp |
| Cập nhật trạng thái đơn | DELIVERED → PENDING (không hợp lệ) | HTTP 400, `INVALID_ORDER_STATUS_TRANSITION` |
| Upload ảnh | File vượt quá 20MB | HTTP 413 |
| Lấy ảnh bìa | `size=thumbnail` | Trả về ảnh đã resize 200px |

### 6.4. Xử lý ngoại lệ

Hệ thống có cơ chế xử lý lỗi tập trung qua `AppException` và `ErrorCode`, đảm bảo mọi lỗi đều trả về định dạng JSON thống nhất.

---

## 7. Kết quả

### 7.1. Những gì đã đạt được

- ✅ Xây dựng hoàn chỉnh **RESTful API** cho hệ thống mua sách trực tuyến.
- ✅ **Bảo mật** với Spring Security + JWT, phân quyền rõ ràng ADMIN/USER.
- ✅ **Quản lý sách** đầy đủ CRUD, upload ảnh bìa, resize ảnh tự động đa kích thước.
- ✅ **Vòng đời đơn hàng** hoàn chỉnh: tạo → thanh toán → xử lý → giao hàng → hoàn thành/hủy.
- ✅ **Giỏ hàng và Wishlist** cho trải nghiệm mua sắm.
- ✅ **Dashboard admin** với thống kê doanh thu ngày/tháng và quản lý tồn kho.
- ✅ **Kiến trúc rõ ràng**, code sạch với Lombok, MapStruct giảm boilerplate.
- ✅ **Validation** đầu vào với Bean Validation, xử lý lỗi tập trung.

### 7.2. Hạn chế hiện tại

- ❌ Chưa có unit test và integration test tự động.
- ❌ Chưa tích hợp cổng thanh toán thực tế (VNPay, Momo).
- ❌ Chưa có tính năng đánh giá / bình luận sách.
- ❌ Lưu trữ ảnh cục bộ, chưa hỗ trợ cloud storage (S3, Cloudinary).
- ❌ Chưa có API documentation (Swagger/OpenAPI).

---

## 8. Hướng Phát triển

### 8.1. Ngắn hạn

- **Viết test tự động:** Bổ sung unit test (JUnit 5, Mockito) cho các service chính và integration test với `@SpringBootTest`.
- **API Documentation:** Tích hợp Springdoc OpenAPI (Swagger UI) để mô tả và thử nghiệm API trực tiếp trên trình duyệt.
- **Tích hợp thanh toán:** Kết nối VNPay hoặc Momo để xử lý thanh toán thực tế.

### 8.2. Trung hạn

- **Cloud Storage:** Chuyển lưu trữ ảnh sang AWS S3 hoặc Cloudinary thay vì lưu cục bộ.
- **Đánh giá sách:** Thêm tính năng user đánh giá và bình luận sách.
- **Tìm kiếm nâng cao:** Tích hợp Elasticsearch cho tìm kiếm full-text hiệu quả hơn.
- **Email notification:** Gửi email xác nhận đơn hàng và thay đổi trạng thái.
- **Caching:** Dùng Redis cache danh sách sách, giảm tải CSDL.

### 8.3. Dài hạn

- **Microservices:** Tách các module (Auth, Book, Order, Payment) thành các service độc lập.
- **Frontend:** Xây dựng giao diện người dùng (React/Vue) kết nối với API.
- **CI/CD Pipeline:** Tự động hóa build, test và deploy với GitHub Actions / Jenkins.
- **Monitoring:** Tích hợp Spring Boot Actuator + Prometheus + Grafana theo dõi hiệu năng.

---

*Báo cáo được tạo dựa trên source code thực tế của dự án BookVerse.*
