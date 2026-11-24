FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY . .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S acorde && adduser -S acorde -G acorde
COPY --from=build /workspace/target/acorde-api-2.0.0-SNAPSHOT.jar /app/acorde-api.jar
USER acorde
EXPOSE 8080
HEALTHCHECK --interval=20s --timeout=4s --retries=5 CMD wget -q --spider http://127.0.0.1:8080/api/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/acorde-api.jar"]
