# WebBanHoaBE

Backend service for Web Ban Hoa using Spring Boot.

## What was added for VUO-52

- Flyway dependency in `pom.xml`.
- Profile-based datasource setup:
: `application-dev.properties` for local H2.
: `application-prod.properties` for Supabase PostgreSQL.
- SQL migration file: `src/main/resources/db/migration/V1__init_schema.sql`.

## Environment variables

Use `.env.sample` as reference:

- `SPRING_PROFILES_ACTIVE`: `dev` or `prod`
- `SUPABASE_DB_URL`: JDBC URL for Supabase PostgreSQL
- `SUPABASE_DB_USER`: database username
- `SUPABASE_DB_PASSWORD`: database password
- `SERVER_PORT`: optional app port

## Run in development profile (H2)

```powershell
$env:SPRING_PROFILES_ACTIVE='dev'
D:\websitebanhoa\WebBanHoaBE\mvnw.cmd -f D:\websitebanhoa\WebBanHoaBE\pom.xml spring-boot:run
```

## Run in production profile (Supabase)

Before running, open Supabase Dashboard -> `Connect` and copy the exact JDBC host/port, username, and password for your chosen connection mode (session pooler/direct).

Never commit real credentials to git. Keep them only in local shell env vars or CI secrets.

```powershell
$env:SPRING_PROFILES_ACTIVE='prod'
$env:SUPABASE_DB_URL='jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require'
$env:SUPABASE_DB_USER='postgres.your_project_ref'
$env:SUPABASE_DB_PASSWORD='your_real_db_password'
D:\websitebanhoa\WebBanHoaBE\mvnw.cmd -f D:\websitebanhoa\WebBanHoaBE\pom.xml spring-boot:run
```

## Flyway verification

After starting with `prod`, verify migrations on Supabase PostgreSQL.

Option 1 (recommended): Supabase Dashboard -> SQL Editor -> run queries below.

Option 2: Any PostgreSQL client connected to the same database.

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Expected business tables from `V1`:

```sql
SELECT * FROM categories LIMIT 5;
SELECT * FROM products LIMIT 5;
```

## API contract and Postman

- API contract document: `API_CONTRACT.md`
- Postman collection: `postman/WebBanHoaBE-Cart.postman_collection.json`

## New ticket coverage

- VUO-69: soft delete for products (`deletedAt`, default filter).
- VUO-70: admin product management APIs.
- VUO-74: Cloudflare R2 media upload/delete/signed URL APIs.
- VUO-77: payment transaction schema and repository usage.
- VUO-78: COD confirmation flow with status guard.
- VUO-80: VietQR checkout + webhook handling.
- VUO-82: SePay checkout + webhook handling.

## Additional environment variables

```powershell
$env:R2_ENABLED='false'
$env:R2_ACCOUNT_ID=''
$env:R2_ACCESS_KEY=''
$env:R2_SECRET_KEY=''
$env:R2_BUCKET=''
$env:R2_REGION='auto'
$env:R2_PUBLIC_BASE_URL=''

$env:VIETQR_API_BASE_URL='https://api.vietqr.io'
$env:VIETQR_WEBHOOK_SECRET=''
$env:VIETQR_SIGNING_SECRET=''

$env:SEPAY_API_BASE_URL='https://my.sepay.vn'
$env:SEPAY_WEBHOOK_SECRET=''
$env:SEPAY_SIGNING_SECRET=''
```

Admin endpoints in this phase use header `X-Role: ADMIN`.

## Cart APIs (VUO-64)

Cart endpoints require `Authorization: Bearer <JWT>` and use `sub` as `userId`.

- `POST /api/cart/items`
	- Body: `{ "productId": 1, "quantity": 2 }`
- `GET /api/cart`
- `PUT /api/cart/items/{id}`
	- Body: `{ "quantity": 3 }`
- `DELETE /api/cart/items/{id}`

Validation included:

- Stock check when adding/updating cart items.
- Price sync using latest product price in cart response.
