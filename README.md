# LoveConnect

LoveConnect is a Java 8 + Spring Boot 2.7 full-stack dating application with JWT authentication, JPA/Hibernate persistence, MySQL schema, React UI, Bootstrap styling, recommendation scoring, likes, chat persistence, subscriptions, notifications, and an admin dashboard.

## Project Structure

```text
LoveConnect/
  backend/
    src/main/java/com/loveconnect/app/
      config/ controller/ dto/ entity/ exception/ repository/ security/ service/ util/
    src/main/resources/application.properties
    src/test/java/com/loveconnect/app/
    pom.xml
  notification-service/
    src/main/java/com/loveconnect/notification/
      controller/ dto/ service/
    src/main/resources/application.properties
    pom.xml
  frontend/
    src/components/ src/pages/ src/services/ src/styles/
    package.json
  database/schema.sql
  docs/API.md
```

## Backend Setup

1. Install Java 8, Maven, and MySQL 8.
2. Create the database:

```bash
mysql -u root -p < database/schema.sql
```

3. Update `backend/src/main/resources/application.properties` with your MySQL username, password, CORS origin, upload directory, and a strong JWT secret.
4. Start the API:

```bash
cd backend
mvn spring-boot:run
```

5. Open Swagger:

```text
http://localhost:8080/swagger-ui.html
```

## Docker Setup

You can run the complete stack with Docker:

```bash
docker compose up --build
```

This starts:

```text
Frontend:             http://localhost:5173
Backend API:          http://localhost:8080
Backend Swagger:      http://localhost:8080/swagger-ui.html
Notification service: http://localhost:8081/api/micro/notifications/health
MySQL:                localhost:3308
```

The Docker stack includes:

- `mysql` with a persistent `loveconnect-mysql-data` volume, exposed on host port `3308` to avoid conflicting with the local MySQL on `3307`.
- `backend` connected to MySQL inside Docker at `mysql:3306`.
- `notification-service` connected from the backend at `notification-service:8081`.
- `frontend` served by Nginx on port `5173`.
- `loveconnect-uploads` volume for uploaded profile media.

If local development servers are already using the same ports, stop them first:

```bash
docker compose down
```

To remove Docker database/uploads data and start completely fresh:

```bash
docker compose down -v
```

## Notification Microservice

LoveConnect includes a small notification microservice that runs separately from the main backend. The backend saves notifications in MySQL and then calls this service for push/email delivery simulation.

Start it in a second terminal:

```bash
cd notification-service
mvn spring-boot:run
```

Endpoints:

```text
GET  http://localhost:8081/api/micro/notifications/health
POST http://localhost:8081/api/micro/notifications/deliver
```

The main backend points to it with:

```properties
app.notification-service.url=http://localhost:8081/api/micro/notifications/deliver
```

## Frontend Setup

1. Install Node.js 20+.
2. Configure the backend API URL. For local development, create `frontend/.env`:

```bash
VITE_API_URL=http://localhost:8080/api
```

For Vercel or another hosted frontend, set `VITE_API_URL` to your public backend URL, for example:

```bash
VITE_API_URL=https://your-backend-domain.com/api
```

Do not use `localhost` for deployed frontend users. In a browser, `localhost` means the user's own computer.

3. Start the UI:

```bash
cd frontend
npm install
npm run dev
```

4. Open:

```text
http://localhost:5173
```

## Deployed Backend Requirement

The React frontend cannot run the Spring Boot API by itself. When you deploy the frontend to Vercel, you must deploy the backend separately, for example to Render, Railway, Fly.io, AWS, Azure, or a VPS.

After backend deployment:

1. Copy the backend public URL.
2. In Vercel Project Settings, add:

```text
VITE_API_URL=https://your-backend-domain.com/api
```

3. Redeploy the Vercel frontend.
4. Add your Vercel domain to backend CORS:

```properties
app.cors.allowed-origins=https://your-vercel-domain.vercel.app,http://localhost:5173,http://127.0.0.1:5173
```

## Production Notes

- Replace the demo JWT secret with a managed secret from your deployment platform.
- Use Flyway or Liquibase before production instead of `spring.jpa.hibernate.ddl-auto=update`.
- Store uploads in S3, Azure Blob Storage, or another object storage service.
- Connect forgot/reset password to signed token storage and email delivery.
- Replace the demo payment reference with Stripe, Razorpay, or your payment provider webhook flow.
- Add moderation workflows for reports, abuse handling, and profile verification.
- Serve the React build through Nginx/CDN and run the backend behind HTTPS.

## Test Commands

```bash
cd backend
mvn test

cd ../frontend
npm run build
```
