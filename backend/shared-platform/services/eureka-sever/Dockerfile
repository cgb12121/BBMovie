# Stage 1: Build the application
FROM amazoncorretto:25-alpine as builder

# Set the working directory
WORKDIR /app

# Copy the Maven wrapper and POM file
COPY .mvn/ .mvn
COPY mvnw .
COPY pom.xml .

# Copy the source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Stage 2: Create the final, lightweight image
FROM amazoncorretto:25-alpine

# Set the working directory
WORKDIR /app

# Copy the application JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set the entrypoint for the container
ENTRYPOINT ["java", "-jar", "app.jar"]
