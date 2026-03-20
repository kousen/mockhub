# Stage 1: Build React frontend
FROM node:22-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

# Stage 2: Build Spring Boot backend with frontend bundled
FROM eclipse-temurin:25-jdk-alpine AS backend-build
WORKDIR /app
COPY backend/gradle/ gradle/
COPY backend/gradlew backend/build.gradle* backend/settings.gradle* ./
RUN ./gradlew dependencies --no-daemon
COPY backend/src/ src/
# Copy React build into Spring Boot static resources
COPY --from=frontend-build /app/frontend/dist/ src/main/resources/static/
RUN ./gradlew bootJar --no-daemon

# Stage 3: Runtime
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
