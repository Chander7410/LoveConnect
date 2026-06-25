# LoveConnect Free Backend Hosting: Back4App Containers

Use this when Render is suspended because the free bandwidth limit is crossed.

## Current Problem

- Frontend can open on Vercel.
- Backend on Render returns `503 Service Unavailable` because the Render workspace is suspended.
- Email OTP code is ready, but it cannot work until the Spring Boot backend is running.

## Recommended Free Option

Use Back4App Containers first because it supports Docker deployment from GitHub.

## Backend Service Settings

Repository:

```text
Chander7410/LoveConnect
```

Branch:

```text
main
```

Dockerfile path:

```text
mongo-firebase-agora-app/backend/Dockerfile
```

Docker build context:

```text
mongo-firebase-agora-app/backend
```

Port:

```text
8080
```

Health check path:

```text
/api/health
```

## Environment Variables

Add these in Back4App container environment variables:

```env
SERVER_PORT=8080
MONGODB_URI=your-mongodb-atlas-uri
APP_CORS_ALLOWED_ORIGINS=https://love-connect-beta.vercel.app,http://localhost:5180,http://127.0.0.1:5180
JWT_SECRET=use-a-long-random-secret
JWT_EXPIRY=86400
ALLOW_DEV_TOKENS=false
FIREBASE_SERVICE_ACCOUNT_JSON=
BREVO_SMTP_HOST=smtp-relay.brevo.com
BREVO_SMTP_PORT=587
BREVO_SMTP_USERNAME=your-brevo-smtp-login
BREVO_SMTP_PASSWORD=your-brevo-smtp-key
MAIL_FROM=supportloveconnect@gmail.com
APP_DEV_OTP_RESPONSE=false
```

If the host provides a `PORT` variable automatically, the backend will now also respect it.

## Frontend Vercel Variable

After Back4App gives you a backend URL, add this in Vercel:

```env
VITE_API_URL=https://your-back4app-backend-url/api
```

Then redeploy the Vercel frontend.

## Runtime Test Override

The frontend now supports a runtime API override from browser console:

```js
localStorage.setItem("loveconnect_api_url", "https://your-back4app-backend-url/api");
location.reload();
```

Clear it with:

```js
localStorage.removeItem("loveconnect_api_url");
location.reload();
```

## Test URLs

Backend health:

```text
https://your-back4app-backend-url/api/health
```

Frontend:

```text
https://love-connect-beta.vercel.app
```

## Go-Live Checks

- `/api/health` returns OK.
- Login page does not show backend unreachable.
- Signup sends Brevo OTP email.
- Signup OTP creates account.
- Forgot password sends Brevo OTP email.
- Reset password works.
- Chat API loads after login.
- Calls still open after login.
