# LoveConnect Production Microservices On Render

This is the production microservice path. The existing `render.yaml` keeps the current monolith safe. Use `render.microservices.yaml` only when you want the separated-container deployment.

## Services

- `loveconnect-auth-service`: signup, login, OTP, forgot password, Google login
- `loveconnect-profile-service`: profiles, search, likes, subscriptions, notifications, safety
- `loveconnect-chat-service`: chat REST APIs and chat WebSocket support
- `loveconnect-call-service`: call history and WebRTC signaling
- `loveconnect-api-gateway`: public Nginx gateway for frontend

## Required Render Environment Values

Set the same values where Render asks for them:

```text
MONGODB_URI=<your MongoDB Atlas connection string>
APP_JWT_SECRET=<one long shared secret, same for all backend services>
FIREBASE_SERVICE_ACCOUNT_JSON=<only if Firebase token verification is required>
BREVO_API_KEY=<Brevo API key for email OTP over HTTPS>
BREVO_SMTP_USERNAME=<optional SMTP login>
BREVO_SMTP_PASSWORD=<optional SMTP key>
GOOGLE_CLIENT_ID=<Google OAuth client id, optional>
GOOGLE_CLIENT_SECRET=<Google OAuth client secret, optional>
```

Important: this Blueprint uses Render `free` web services to avoid payment setup. Free services can be slow, sleep after inactivity, and share the monthly free-hour pool. Keep `BREVO_API_KEY` set so OTP emails use Brevo HTTPS API instead of SMTP.

## Render Deploy Steps

1. Open Render Dashboard.
2. Create a new Blueprint from the GitHub repo.
3. Use this Blueprint file path:

```text
render.microservices.yaml
```

4. Paste `MONGODB_URI`.
5. Paste the same `APP_JWT_SECRET` for every backend service.
6. Paste `BREVO_API_KEY` for `loveconnect-auth-service`.
7. Deploy.

After deploy, the public API gateway should be:

```text
https://loveconnect-api-gateway.onrender.com/api
```

If Render gives a different gateway URL, use that exact URL.

## Vercel Update

Set this Vercel frontend environment variable:

```text
VITE_API_URL=https://loveconnect-api-gateway.onrender.com/api
```

Then redeploy Vercel.

## Production Test

```bash
curl https://loveconnect-api-gateway.onrender.com/api/health
```

Expected:

```json
{"status":"ok","database":"mongodb"}
```

Then test:

- Signup OTP
- Login
- Profile search
- Chat
- Audio/video call popup
- WebRTC answer/remote video flow

## Rollback

If anything goes wrong, keep Vercel `VITE_API_URL` pointed to the old backend:

```text
https://loveconnect-mddv.onrender.com/api
```

The old monolith deployment is still supported by `render.yaml`.
