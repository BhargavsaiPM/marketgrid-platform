# Stage 1: Build executable JAR using Maven & JDK 17
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy parent POM for dependency resolution
COPY pom.xml .

# Copy service module files
COPY vendor-service/pom.xml vendor-service/
COPY vendor-service/src vendor-service/src

# Package JAR
WORKDIR /build/vendor-service
RUN mvn clean package -DskipTests

# Stage 2: Minimal runtime image using Eclipse Temurin JRE 17 Alpine
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install curl for container healthchecks
RUN apk add --no-cache curl

# Copy built JAR from stage 1
COPY --from=build /build/vendor-service/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
