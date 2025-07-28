# Stage 1: Build
FROM --platform=linux/amd64 gradle:8.4.0-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle gradle
RUN gradle dependencies --no-daemon
COPY . .
RUN gradle build -x test --no-daemon

# Stage 2: Run
FROM --platform=linux/amd64 eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
# Use non-root user for security
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring
ENTRYPOINT ["java", "-jar", "app.jar"]
