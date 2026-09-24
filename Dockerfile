# Stage 1: Build JAR
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY BookVerse/pom.xml .
COPY BookVerse/src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run application
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/BookVerse-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "app.jar"]
