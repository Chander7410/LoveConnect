# LoveConnect Microservices Docker Setup

This setup keeps the existing working monolith safe and adds service-profile based microservices.

## Containers

- `frontend`: React/Vite UI served by Nginx on `http://localhost:5180`
- `api-gateway`: Nginx gateway on `http://localhost:8180`
- `auth-service`: signup, login, Google login, email OTP, forgot password
- `profile-service`: profile, search, recommendations, likes, subscriptions, notifications, safety compatibility APIs
- `chat-service`: chat history and message send APIs
- `call-service`: call REST APIs and WebRTC WebSocket/STOMP signaling
- `mongo`: MongoDB database

## Routing

The frontend calls one URL:

```text
http://localhost:8180/api
```

Gateway routes:

```text
/api/auth/**          -> auth-service
/api/profiles/**      -> profile-service
/api/profile/**       -> profile-service
/api/search/**        -> profile-service
/api/likes/**         -> profile-service
/api/notifications/** -> profile-service
/api/subscriptions/** -> profile-service
/api/safety/**        -> profile-service
/api/chats/**         -> chat-service
/api/chat/**          -> chat-service
/api/calls/**         -> call-service
/ws/**                -> call-service
```

## Spring Security

Each backend container runs the same Spring Security filter chain.

- Public: health, login/signup/OTP/reset public auth endpoints, Swagger, WebSocket handshake
- Protected: profile, search, chat, call, notifications, subscriptions, safety
- All services must use the same `APP_JWT_SECRET`

## Run

```bash
copy .env.microservices.example .env
docker compose -f docker-compose.microservices.yml up --build
```

Open:

```text
http://localhost:5180
```

Health:

```bash
curl http://localhost:8180/api/health
```

## Keep Existing Production Safe

The existing Render/Vercel deployment can continue using the default monolith profile.
No existing routes were removed or renamed.
