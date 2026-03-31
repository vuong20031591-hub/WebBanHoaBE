# Postman Collections

Thư mục này chứa các Postman collections để test APIs của WebBanHoaBE.

## Collections

### 1. WebBanHoaBE-Auth-User-Address.postman_collection.json
Test các APIs liên quan đến Authentication, User Profile và Address Management.

**Endpoints:**
- Authentication: Register, Login (Admin & User)
- User Profile: Get profile, Update profile, Change password
- Address Management: CRUD addresses, Set default address

### 2. WebBanHoaBE-Orders.postman_collection.json
Test các APIs liên quan đến Orders và Admin Order Management.

**Endpoints:**
- User Orders: Create order from cart, Get order detail, Confirm COD
- Admin Order Management: List orders with filters, Update status, Get statistics

### 3. WebBanHoaBE-Cart.postman_collection.json
Test các APIs liên quan đến Shopping Cart.

**Endpoints:**
- Cart: Add item, Get cart, Update quantity, Remove item

### 4. WebBanHoaBE-Admin-Payments.postman_collection.json
Test các APIs liên quan đến Admin Product Management và Payments.

**Endpoints:**
- Admin Products: CRUD products, Soft delete, Restore, Update stock
- Payments: VietQR checkout, SePay checkout, Webhooks, Reconciliation

## Cách sử dụng

### 1. Import vào Postman
- Mở Postman
- Click **Import** > **Choose Files**
- Chọn file `.json` cần import
- Click **Import**

### 2. Cấu hình Environment Variables
Mỗi collection có các biến:
- `baseUrl`: http://localhost:8080 (default)
- `adminToken`: JWT token của admin (tự động set sau khi login)
- `userToken`: JWT token của user (tự động set sau khi login)

### 3. Chạy test
1. Khởi động server: `./mvnw spring-boot:run`
2. Chạy folder **Setup - Login** trước để lấy tokens
3. Chạy các requests khác theo thứ tự

### 4. Test Credentials
**Admin:**
- Email: admin@florastore.com
- Password: admin123

**User:**
- Email: user@florastore.com
- Password: user123

## Test Flow Recommendations

### Flow 1: User Registration & Profile
1. Register User
2. Login User
3. Get My Profile
4. Update My Profile
5. Change Password

### Flow 2: Address Management
1. Login User
2. Create Address
3. Get All Addresses
4. Update Address
5. Set Default Address
6. Delete Address

### Flow 3: Shopping & Order
1. Login User
2. Add items to cart (use Cart collection)
3. Get Cart
4. Create Order from Cart
5. Get Order Detail
6. Confirm COD Order (if payment method is COD)

### Flow 4: Admin Order Management
1. Login Admin
2. Get All Orders
3. Filter Orders by Status/Date/User
4. Update Order Status
5. Get Order Statistics

## Notes
- Tất cả requests đều có test scripts để validate response
- Tokens được tự động lưu vào collection variables sau khi login
- Order ID được tự động lưu sau khi tạo order để dùng cho các requests tiếp theo
