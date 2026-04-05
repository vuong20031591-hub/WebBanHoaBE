# Huong Dan Deploy WebBanHoa len domain .eu.cc (Gname)

Tai lieu nay huong dan deploy full stack cho du an:
- FE: Next.js (`WebBanHoaFE`)
- BE: Spring Boot (`WebBanHoaBE`)
- Domain: `.eu.cc` mua/quan ly tai Gname

Huong dan theo mo hinh VPS Ubuntu + Nginx + Let's Encrypt.

## 1. Kien truc de xuat

Vi du domain ban dang ky la `florastore.eu.cc`:
- FE public: `https://florastore.eu.cc`
- FE www: `https://www.florastore.eu.cc`
- BE API: `https://api.florastore.eu.cc`
- BE internal port: `127.0.0.1:8080`
- FE internal port: `127.0.0.1:3000`

## 2. Luu y ve domain .eu.cc

Theo trang Gname, `.eu.cc` la second-level domain project (khong phai ICANN TLD), nhung van su dung duoc day du chuc nang DNS/deploy web nhu domain thong thuong.

## 3. Chuan bi server

Khuyen nghi:
- Ubuntu 22.04/24.04
- RAM toi thieu 2 GB
- Da mo cong 80, 443

Dang nhap VPS va cai dat:

```bash
sudo apt update
sudo apt install -y git nginx certbot python3-certbot-nginx curl unzip openjdk-21-jre-headless
```

Cai Node.js 20 LTS (cho Next.js):

```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
node -v
npm -v
java -version
```

## 4. Cau hinh DNS tren Gname

Vao DNS Management cua domain tai Gname va tao cac ban ghi:

| Type | Host | Value | TTL |
|---|---|---|---|
| A | @ | `YOUR_VPS_PUBLIC_IP` | Auto |
| A | www | `YOUR_VPS_PUBLIC_IP` | Auto |
| A | api | `YOUR_VPS_PUBLIC_IP` | Auto |

Sau khi luu, doi DNS propagate (thuong 5-30 phut, co the lau hon).

Kiem tra:

```bash
nslookup florastore.eu.cc
nslookup api.florastore.eu.cc
```

## 5. Deploy Backend (Spring Boot)

### 5.1. Lay source va build

```bash
sudo mkdir -p /opt/webbanhoa
sudo chown -R $USER:$USER /opt/webbanhoa
cd /opt/webbanhoa
git clone <YOUR_GIT_REMOTE_BE> WebBanHoaBE
cd WebBanHoaBE
./mvnw clean package -DskipTests
```

### 5.2. Tao file env cho BE

```bash
cp .env.example .env
nano .env
```

Can cap nhat it nhat cac bien sau:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS=https://florastore.eu.cc,https://www.florastore.eu.cc`
- `R2_*` (neu dung upload media)
- `VIETQR_*`, `SEPAY_*`, `BANK_*` (neu dung thanh toan)
- `WEBHOOK_IP_WHITELIST` (neu ban bat gioi han IP webhook)

### 5.3. Tao service systemd cho BE

```bash
sudo tee /etc/systemd/system/webbanhoa-be.service > /dev/null <<'EOF'
[Unit]
Description=WebBanHoa Spring Boot Backend
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/webbanhoa/WebBanHoaBE
Environment=SPRING_PROFILES_ACTIVE=prod
Environment=SERVER_PORT=8080
ExecStart=/usr/bin/java -jar /opt/webbanhoa/WebBanHoaBE/target/web-ban-hoa-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
```

Neu user VPS cua ban khong phai `ubuntu`, sua lai gia tri `User=` cho dung.

Khoi dong service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable webbanhoa-be
sudo systemctl restart webbanhoa-be
sudo systemctl status webbanhoa-be --no-pager
```

Kiem tra local API:

```bash
curl http://127.0.0.1:8080/api/categories
```

## 6. Deploy Frontend (Next.js)

### 6.1. Lay source va build

```bash
cd /opt/webbanhoa
git clone <YOUR_GIT_REMOTE_FE> WebBanHoaFE
cd WebBanHoaFE
npm ci
```

### 6.2. Tao env production cho FE

```bash
cat > .env.production <<'EOF'
NEXT_PUBLIC_API_BASE_URL=https://api.florastore.eu.cc
NEXT_PUBLIC_SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=YOUR_SUPABASE_ANON_KEY
EOF
```

Build:

```bash
npm run build
```

### 6.3. Tao service systemd cho FE

```bash
sudo tee /etc/systemd/system/webbanhoa-fe.service > /dev/null <<'EOF'
[Unit]
Description=WebBanHoa Next.js Frontend
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/webbanhoa/WebBanHoaFE
Environment=NODE_ENV=production
ExecStart=/usr/bin/npm run start -- --hostname 127.0.0.1 --port 3000
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
```

Khoi dong service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable webbanhoa-fe
sudo systemctl restart webbanhoa-fe
sudo systemctl status webbanhoa-fe --no-pager
```

Test FE local:

```bash
curl -I http://127.0.0.1:3000
```

## 7. Cau hinh Nginx reverse proxy

```bash
sudo tee /etc/nginx/sites-available/webbanhoa > /dev/null <<'EOF'
server {
    listen 80;
    server_name florastore.eu.cc www.florastore.eu.cc;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen 80;
    server_name api.florastore.eu.cc;

    client_max_body_size 20m;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF

sudo ln -s /etc/nginx/sites-available/webbanhoa /etc/nginx/sites-enabled/webbanhoa
sudo nginx -t
sudo systemctl reload nginx
```

## 8. Bat HTTPS bang Let's Encrypt

```bash
sudo certbot --nginx \
  -d florastore.eu.cc \
  -d www.florastore.eu.cc \
  -d api.florastore.eu.cc
```

Kiem tra tu dong gia han:

```bash
sudo systemctl status certbot.timer --no-pager
```

## 9. Cau hinh callback/webhook sau khi len domain that

Cap nhat tren dashboard doi tac thanh toan:
- VietQR webhook URL: `https://api.florastore.eu.cc/api/payments/vietqr/webhook`
- SePay webhook URL: `https://api.florastore.eu.cc/api/payments/sepay/webhook`

Neu dung SePay auth header API key, dam bao gia tri khop bien `SEPAY_WEBHOOK_SECRET` tren BE.

## 10. Checklist xac nhan sau deploy

1. Mo `https://florastore.eu.cc` truy cap duoc FE.
2. Goi `https://api.florastore.eu.cc/api/categories` tra JSON.
3. Dang ky/dang nhap tren FE thanh cong.
4. Them vao gio hang, tao don hang thanh cong.
5. Neu dung VietQR/SePay: webhook ve thanh cong (status 200).
6. Kiem tra log service:

```bash
sudo journalctl -u webbanhoa-be -f
sudo journalctl -u webbanhoa-fe -f
```

## 11. Lenh cap nhat version moi

Backend:

```bash
cd /opt/webbanhoa/WebBanHoaBE
git pull
./mvnw clean package -DskipTests
sudo systemctl restart webbanhoa-be
```

Frontend:

```bash
cd /opt/webbanhoa/WebBanHoaFE
git pull
npm ci
npm run build
sudo systemctl restart webbanhoa-fe
```

## 12. Su co thuong gap

- FE khong goi duoc API (CORS):
  - Kiem tra `CORS_ALLOWED_ORIGINS` tren BE co dung domain HTTPS.
- FE bao loi ket noi localhost:
  - Kiem tra `NEXT_PUBLIC_API_BASE_URL` trong `.env.production` da la `https://api.<domain>`.
- SSL khong cap duoc:
  - Kiem tra DNS da tro dung IP VPS va port 80/443 da mo.
- SePay webhook bi 401:
  - Kiem tra `SEPAY_WEBHOOK_SECRET`, header `Authorization`, va IP whitelist.

---

Ban co the doi ten mien mau `florastore.eu.cc` thanh domain that cua ban theo cung cau truc.
