FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY mongo-firebase-agora-app/backend/pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY mongo-firebase-agora-app/backend/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/mongo-firebase-agora-backend-1.0.0.jar app.jar
ENV APP_CORS_ALLOWED_ORIGINS=https://love-connect-beta.vercel.app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
