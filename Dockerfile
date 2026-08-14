FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar
COPY --from=build /app/target/*.jar app.jar

# Expose default port
EXPOSE 8082

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
