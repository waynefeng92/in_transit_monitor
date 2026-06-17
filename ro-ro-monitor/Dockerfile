# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r roro && useradd -r -g roro roro
RUN mkdir -p /app/logs /app/uploads && chown -R roro:roro /app
COPY --from=build /app/target/*.jar app.jar
USER roro
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
