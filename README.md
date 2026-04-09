# WebBanHoaBE

Backend service for Web Ban Hoa using Spring Boot.

## Scope covered

- VUO-62: category and product browsing support.
- VUO-64: authenticated cart APIs.
- VUO-69: soft delete for products.
- VUO-70: admin product management APIs.
- VUO-74: Cloudflare R2 media upload/delete/signed URL APIs.
- VUO-77: payment transaction persistence and reconciliation.
- VUO-78: order creation from cart and COD confirmation flow.
- VUO-80: VietQR checkout and webhook handling.
- VUO-82: SePay checkout and webhook handling.

## Environment variables

Use `.env.example` as reference.

Core:

- `SPRING_PROFILES_ACTIVE`: `dev` or `prod`
- `DB_URL`: JDBC URL for Supabase PostgreSQL
- `DB_USERNAME`: database username
- `DB_PASSWORD`: database password
- `SERVER_PORT`: optional app port
- `DB_MIN_IDLE`: optional, defaults to `0`
- `DB_MAX_POOL_SIZE`: optional, defaults to `3`

Media:

- `R2_ENABLED`
- `R2_ACCOUNT_ID`
- `R2_ACCESS_KEY`
- `R2_SECRET_KEY`
- `R2_BUCKET`
- `R2_REGION`
- `R2_PUBLIC_BASE_URL`
- `MEDIA_IMAGE_MAX_ORIGINAL_SIZE_BYTES`
- `MEDIA_IMAGE_MAX_WIDTH`
- `MEDIA_IMAGE_MAX_HEIGHT`
- `MEDIA_IMAGE_JPEG_QUALITY`
- `MEDIA_IMAGE_CACHE_CONTROL`

Payments:

- `VIETQR_API_BASE_URL`
- `VIETQR_WEBHOOK_SECRET`
- `VIETQR_SIGNING_SECRET`
- `SEPAY_API_BASE_URL`
- `SEPAY_WEBHOOK_SECRET`
- `SEPAY_SIGNING_SECRET`

## Run in development profile

```powershell
$env:SPRING_PROFILES_ACTIVE='dev'
D:\websitebanhoa\WebBanHoaBE\mvnw.cmd -f D:\websitebanhoa\WebBanHoaBE\pom.xml spring-boot:run
```

## Run in production profile

```powershell
$env:SPRING_PROFILES_ACTIVE='prod'
$env:DB_URL='jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require'
$env:DB_USERNAME='postgres.your_project_ref'
$env:DB_PASSWORD='your_real_db_password'
D:\websitebanhoa\WebBanHoaBE\mvnw.cmd -f D:\websitebanhoa\WebBanHoaBE\pom.xml spring-boot:run
```

## Verification

- API contract: `API_CONTRACT.md`
- Postman collection: `postman/WebBanHoaBE-Cart.postman_collection.json`
- Frontend manual test bench: `D:\websitebanhoa\WebBanHoaFE\app\workspace\page.tsx`

Admin endpoints in this phase use header `X-Role: ADMIN`.
Cart and order/payment APIs use `Authorization: Bearer <JWT>` and read `sub` as `userId`.

## Troubleshooting

- If startup fails with `MaxClientsInSessionMode: max clients reached`, the
  current Supabase URL is hitting the session pool limit before Flyway can
  connect.
- In that case, reduce pool usage with `DB_MIN_IDLE=0` and a small
  `DB_MAX_POOL_SIZE`, then switch `DB_URL` away from the exhausted session
  pooler endpoint to the transaction-pooling or direct connection string from
  your Supabase dashboard.
