# LoveConnect Oracle Always Free Backend

This setup runs only the Spring Boot backend on an Oracle Always Free VM. Keep the frontend on Vercel.

## Why This Fixes The Slow Backend

Render free web services sleep after inactivity. An Oracle Always Free VM can keep Docker running all the time, so the backend does not cold-start on every first request.

## Oracle VM Requirements

- Image: Ubuntu 22.04 or 24.04
- Shape: Always Free eligible VM
- Open inbound ports in Oracle security list:
  - `22` for SSH
  - `80` for Caddy HTTP challenge
  - `443` for HTTPS API

## Deploy Steps

SSH into the Oracle VM, then run:

```bash
curl -fsSL https://raw.githubusercontent.com/Chander7410/LoveConnect/main/deploy/oracle/setup-oracle-ubuntu.sh -o setup-oracle-ubuntu.sh
chmod +x setup-oracle-ubuntu.sh
./setup-oracle-ubuntu.sh
```

Log out and SSH in again so Docker group permissions apply.

```bash
git clone https://github.com/Chander7410/LoveConnect.git
cd LoveConnect/deploy/oracle
cp .env.example .env
nano .env
```

Set the real values in `.env`:

```bash
MONGODB_URI=...
APP_JWT_SECRET=...
FIREBASE_SERVICE_ACCOUNT_JSON=...
BREVO_SMTP_USERNAME=...
BREVO_SMTP_PASSWORD=...
BREVO_API_KEY=...
MAIL_FROM=chandershekhar78458@gmail.com
APP_DOMAIN=loveconnect-api.YOUR_PUBLIC_IP.sslip.io
```

Start backend:

```bash
docker compose up -d --build
```

Check logs:

```bash
docker compose logs -f backend
```

Test health:

```bash
curl https://loveconnect-api.YOUR_PUBLIC_IP.sslip.io/api/health
```

Expected:

```json
{"status":"ok","database":"mongodb"}
```

## Switch Vercel Frontend

Only after the Oracle backend health check passes, set this Vercel environment variable:

```bash
VITE_API_URL=https://loveconnect-api.YOUR_PUBLIC_IP.sslip.io/api
```

Redeploy Vercel after changing it.

## Rollback

If anything fails, keep using the current Render backend:

```bash
VITE_API_URL=https://loveconnect-mddv.onrender.com/api
```
