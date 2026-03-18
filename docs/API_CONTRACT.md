# API Contract - Website Bán Hoa

## Thông tin chung

| Thuộc tính | Giá trị |
|---|---|
| Base URL (dev) | `http://localhost:8080` |
| Content-Type | `application/json` |
| CORS allowed origin | `http://localhost:3000` |
| CORS allowed methods | `GET, POST, PUT, DELETE` |

---

## 1. GET /api/products/search

Tìm kiếm và lọc sản phẩm với phân trang.

### Query Parameters

| Tham số | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `name` | String | Không | Tìm theo tên (case-insensitive, contains) |
| `minPrice` | Long | Không | Giá tối thiểu (VNĐ) |
| `maxPrice` | Long | Không | Giá tối đa (VNĐ) |
| `categoryId` | Long | Không | ID danh mục |
| `page` | Integer | Không | Số trang, bắt đầu từ 0 (default: `0`) |
| `size` | Integer | Không | Số item mỗi trang (default: `10`) |
| `sort` | String | Không | Field sắp xếp (default: `id,asc`) |

**Lưu ý filter giá:**
- Chỉ `minPrice` → lấy SP có giá >= minPrice
- Chỉ `maxPrice` → lấy SP có giá <= maxPrice
- Cả hai → lấy SP trong khoảng [min, max] (tự swap nếu min > max)
- Không truyền → không filter giá

### Response 200 OK

```json
{
  "content": [
    {
      "id": 1,
      "name": "Bó hồng đỏ cổ điển",
      "price": 350000.00,
      "description": "Bó 20 bông hồng đỏ tươi",
      "imageUrl": "hong-do-co-dien.jpg",
      "categoryName": "Hoa hồng"
    }
  ],
  "totalElements": 20,
  "totalPages": 2,
  "currentPage": 0,
  "size": 10
}
```

### Ví dụ Request

```
# Tất cả sản phẩm
GET /api/products/search

# Filter theo tên
GET /api/products/search?name=hồng

# Filter theo khoảng giá
GET /api/products/search?minPrice=200000&maxPrice=500000

# Filter theo danh mục
GET /api/products/search?categoryId=1

# Kết hợp nhiều filter
GET /api/products/search?name=hồng&minPrice=300000&maxPrice=800000&categoryId=1

# Phân trang + sắp xếp
GET /api/products/search?page=1&size=5&sort=price,asc
```

---

## 2. GET /api/categories

Lấy danh sách tất cả danh mục.

### Query Parameters

Không có.

### Response 200 OK

```json
[
  {
    "id": 1,
    "name": "Hoa hồng",
    "description": null
  },
  {
    "id": 2,
    "name": "Hoa cưới",
    "description": null
  }
]
```

### Ví dụ Request

```
GET /api/categories
```

---

## 3. Error Response Convention

Spring Boot trả lỗi mặc định theo format sau:

### 400 Bad Request (param sai kiểu dữ liệu)

```json
{
  "timestamp": "2026-03-18T10:00:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "path": "/api/products/search"
}
```

### 404 Not Found

```json
{
  "timestamp": "2026-03-18T10:00:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "path": "/api/unknown"
}
```

### 500 Internal Server Error

```json
{
  "timestamp": "2026-03-18T10:00:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/products/search"
}
```

> **Lưu ý cho FE/QA:** Response trống (empty results) luôn trả `200 OK` với `content: []` và `totalElements: 0`, không bao giờ trả `404`.

---

## 4. Data Model

### ProductDTO

| Field | Kiểu | Mô tả |
|---|---|---|
| `id` | Long | ID sản phẩm |
| `name` | String | Tên sản phẩm |
| `price` | BigDecimal | Giá (VNĐ) |
| `description` | String | Mô tả (nullable) |
| `imageUrl` | String | Tên file ảnh (nullable) |
| `categoryName` | String | Tên danh mục (nullable) |

### CategoryResponse

| Field | Kiểu | Mô tả |
|---|---|---|
| `id` | Long | ID danh mục |
| `name` | String | Tên danh mục |
| `description` | String | Mô tả (nullable) |

### PagedResponse\<T\>

| Field | Kiểu | Mô tả |
|---|---|---|
| `content` | List\<T\> | Danh sách item trang hiện tại |
| `totalElements` | long | Tổng số item |
| `totalPages` | int | Tổng số trang |
| `currentPage` | int | Trang hiện tại (bắt đầu từ 0) |
| `size` | int | Kích thước trang |
