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

```powershell
$env:SPRING_PROFILES_ACTIVE='prod'
$env:SUPABASE_DB_URL='jdbc:postgresql://db.your-project-ref.supabase.co:5432/postgres'
$env:SUPABASE_DB_USER='postgres'
$env:SUPABASE_DB_PASSWORD='your_password_here'
D:\websitebanhoa\WebBanHoaBE\mvnw.cmd -f D:\websitebanhoa\WebBanHoaBE\pom.xml spring-boot:run
```

## Flyway verification

After starting with `prod`, verify migrations on PostgreSQL:

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
