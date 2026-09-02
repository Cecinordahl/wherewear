# Multi-stage build: compile with Maven, then run on a slim JRE.
# This is what Render's Docker runtime builds and runs (see ../render.yaml).

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/backend-0.1.0.jar app.jar

# Render sets $PORT; application.yml already reads it via ${PORT:8080}.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
