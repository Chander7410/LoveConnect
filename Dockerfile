FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY backend/pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY backend/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN mkdir -p /app/uploads
COPY --from=build /app/target/loveconnect-backend-1.0.0.jar app.jar
ENV SPRING_PROFILES_ACTIVE=dev
ENV APP_CORS_ALLOWED_ORIGINS=https://love-connect-beta.vercel.app
ENV APP_UPLOAD_DIR=/app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
