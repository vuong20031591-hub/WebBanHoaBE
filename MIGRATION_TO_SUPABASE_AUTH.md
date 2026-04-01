# Migration Guide: Custom Auth to Supabase Auth

## Overview

Nhánh `feature/merge-dev-features` đã loại bỏ custom auth system và chuyển sang Supabase Auth hoàn toàn.

## Breaking Changes

### Removed Endpoints

Custom auth endpoints đã bị xóa:
- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/logout

### Removed Files

Sau khi merge PR này vào dev, cần xóa các files sau:

**Controllers:**
- src/main/java/com/florastore/web_ban_hoa/controller/AuthController.java
- src/main/java/com/florastore/web_ban_hoa/controller/UserController.java (nếu chỉ dùng cho custom auth)

**Services:**
- src/main/java/com/florastore/web_ban_hoa/service/AuthService.java
- src/main/java/com/florastore/web_ban_hoa/service/impl/AuthServiceImpl.java
- src/main/java/com/florastore/web_ban_hoa/service/UserService.java (nếu chỉ dùng cho custom auth)
- src/main/java/com/florastore/web_ban_hoa/service/impl/UserServiceImpl.java (nếu chỉ dùng cho custom auth)

**Security:**
- src/main/java/com/florastore/web_ban_hoa/security/JwtUtil.java
- src/main/java/com/florastore/web_ban_hoa/security/JwtAuthenticationFilter.java
- src/main/java/com/florastore/web_ban_hoa/config/SecurityConfig.java

**Entities:**
- src/main/java/com/florastore/web_ban_hoa/entity/User.java (nếu không dùng cho mục đích khác)
- src/main/java/com/florastore/web_ban_hoa/entity/Role.java

**Repositories:**
- src/main/java/com/florastore/web_ban_hoa/repository/UserRepository.java (nếu không dùng cho mục đích khác)

**DTOs:**
- src/main/java/com/florastore/web_ban_hoa/dto/AuthResponse.java
- src/main/java/com/florastore/web_ban_hoa/dto/LoginRequest.java
- src/main/java/com/florastore/web_ban_hoa/dto/RegisterRequest.java
- src/main/java/com/florastore/web_ban_hoa/dto/ChangePasswordRequest.java
- src/main/java/com/florastore/web_ban_hoa/dto/UpdateProfileRequest.java (nếu không dùng cho mục đích khác)
- src/main/java/com/florastore/web_ban_hoa/dto/UserResponse.java (nếu không dùng cho mục đích khác)

**Migrations:**
- src/main/resources/db/migration/V2__create_users_table.sql

## New Authentication Flow

### Frontend Changes Required

Frontend phải chuyển sang sử dụng Supabase Auth API:

**Register:**
```javascript
const { data, error } = await supabase.auth.signUp({
  email: 'user@example.com',
  password: 'password123'
})
```

**Login:**
```javascript
const { data, error } = await supabase.auth.signInWithPassword({
  email: 'user@example.com',
  password: 'password123'
})
```

**Logout:**
```javascript
const { error } = await supabase.auth.signOut()
```

**Get JWT Token:**
```javascript
const { data: { session } } = await supabase.auth.getSession()
const token = session?.access_token
```

### Backend API Calls

Tất cả API calls từ frontend phải include JWT token từ Supabase:

```javascript
const response = await fetch('http://localhost:8080/api/cart', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
})
```

## User Management

### User Data Storage

User data giờ được lưu trong Supabase:
- Authentication: `auth.users` table (managed by Supabase)
- User metadata: `auth.users.raw_user_meta_data` (JSON field)
- User ID: UUID format từ Supabase

### User ID Format

**Old (Custom Auth):**
- Type: String (email hoặc custom ID)
- Example: "user@example.com"

**New (Supabase Auth):**
- Type: UUID String
- Example: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"

### Database Schema Changes

Các tables sử dụng `user_id` giờ expect UUID format:
- orders.user_id
- carts.user_id
- addresses.user_id

## Security

### JWT Validation

Backend giờ validate JWT từ Supabase:
- Algorithm: ES256 (asymmetric)
- Public key: Lấy từ Supabase JWKS endpoint
- Issuer: https://[project-id].supabase.co/auth/v1

### RLS Policies

Database sử dụng Row Level Security với `auth.uid()`:
```sql
CREATE POLICY "Users can view own cart"
ON carts FOR SELECT
USING (auth.uid()::text = user_id);
```

## Testing

### Test Credentials

Supabase test users:
- testuser@florastore.com / Test@123456
- admin@florastore.com / Admin@123456

### Admin Role

Admin role được set trong Supabase metadata:
```sql
UPDATE auth.users 
SET raw_user_meta_data = jsonb_set(
  raw_user_meta_data, 
  '{role}', 
  '"ADMIN"'
)
WHERE email = 'admin@florastore.com';
```

Backend check admin role qua X-Role header hoặc JWT claims.

## Rollback Plan

Nếu cần rollback về custom auth:
1. Revert PR này
2. Restore các files đã xóa từ git history
3. Update frontend về custom auth endpoints
4. Migrate user data từ Supabase về local database

## Support

Nếu có vấn đề trong quá trình migration, liên hệ team lead hoặc tạo issue trên GitHub.
