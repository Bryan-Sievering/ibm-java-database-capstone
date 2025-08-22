# Step 1: Use Maven with JDK 21 to build the app
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Step 2: Use lightweight JRE 21 for running the app
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/target/back-end-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]