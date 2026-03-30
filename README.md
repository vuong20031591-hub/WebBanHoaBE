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
