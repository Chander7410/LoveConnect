# LoveConnect API Documentation

Swagger UI is available at `http://localhost:8080/swagger-ui.html` after the backend starts.
OpenAPI JSON is available at `http://localhost:8080/v3/api-docs`.

## Authentication

All protected endpoints require:

```http
Authorization: Bearer <jwt>
```

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/auth/register` | Register a user with name, email, mobile, gender, age, location, password |
| POST | `/api/auth/login` | Login and receive JWT |
| POST | `/api/auth/forgot-password` | Start password reset flow |
| POST | `/api/auth/reset-password` | Complete password reset flow |
| GET | `/api/profile/me` | Current user profile |
| GET | `/api/profile/{userId}` | Public profile by user id |
| PUT | `/api/profile/me` | Update bio, education, profession, city, interests |
| POST | `/api/profile/picture` | Upload primary profile picture |
| POST | `/api/profile/photos` | Upload additional photo |
| GET | `/api/search` | Search by age, gender, city, interest |
| GET | `/api/search/recommendations` | Recommendation list with match score |
| POST | `/api/likes` | Like or dislike a profile |
| GET | `/api/likes/received` | View received likes |
| POST | `/api/chat/messages` | Send persisted chat message |
| GET | `/api/chat/conversation/{userId}` | Conversation history |
| POST | `/api/chat/messages/{messageId}/read` | Mark message as read |
| GET | `/api/notifications` | List notifications |
| POST | `/api/subscriptions` | Create free or premium subscription |
| GET | `/api/subscriptions` | Subscription history |
| GET | `/api/admin/dashboard` | Admin metrics |
| GET | `/api/admin/users` | Manage users |
| PATCH | `/api/admin/users/{userId}/block?blocked=true` | Block or unblock user |

## WebSocket

STOMP endpoint: `/ws`

Broker destinations:

- Client sends to `/app/chat.send`
- Server can deliver user-specific messages to `/user/queue/messages`
