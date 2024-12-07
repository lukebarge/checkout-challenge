# Build stage
FROM gradle:8.8-jdk21 AS build

WORKDIR /app
COPY . .
RUN gradle build --no-daemon

# Runtime stage
FROM amazoncorretto:21-alpine

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 7070

ENTRYPOINT ["java", "-jar", "app.jar"]