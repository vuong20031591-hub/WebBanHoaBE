# Setup Instructions

## Backend Configuration

### 1. Copy .env Template

```bash
cp .env.example .env
```

### 2. Edit .env File

Mở `.env` và điền credentials thật:

```properties
DB_URL=jdbc:postgresql://YOUR_HOST:5432/postgres?sslmode=require
DB_USERNAME=postgres
DB_PASSWORD=your_password
CORS_ALLOWED_ORIGINS=http://localhost:3000
JWT_SECRET=your_jwt_secret
# ... các biến khác
```

**Lưu ý:** File `.env` phải theo format properties (`KEY=value`), không dùng `export`.

### 3. Run Application

```bash
./mvnw spring-boot:run
```

Hoặc trên Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

### 4. Verify

Backend sẽ chạy tại `http://localhost:8080`

Test endpoint:
```bash
curl http://localhost:8080/api/categories
```

## How It Works

- `application.properties` chứa structure và defaults
- `.env` file chứa secrets (git-ignored)
- Spring Boot tự động import `.env` qua `spring.config.import`
- Env variables có thể override từ OS

## Security Notes

- **KHÔNG BAO GIỜ** commit file `.env` vào git
- File `.env` đã được gitignore
- Chỉ commit `.env.example` (template không có secrets)

## Production Deployment

Khi deploy production:

1. **Không dùng** `.env` file
2. Set environment variables trên hosting platform:
   - Heroku: `heroku config:set DB_URL=...`
   - Railway: Dashboard → Variables
   - AWS: Parameter Store / Secrets Manager
3. Update `CORS_ALLOWED_ORIGINS` với domain thật
4. Đảm bảo `JWT_SECRET` đủ mạnh (minimum 256 bits)
