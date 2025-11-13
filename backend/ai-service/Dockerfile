# STAGE 1: Build
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# --- 1. Build the 'common' module first ---
# This path is relative to the 'backend' context
COPY bbmovie-common/pom.xml ./common-build/pom.xml
RUN mvn -B -f common-build/pom.xml -q dependency:go-offline

COPY bbmovie-common ./common-build
RUN mvn -B -f common-build/pom.xml -DskipTests clean install

# --- 2. Build the main microservice ---
# Copy the main application's POM and wrapper
# These paths are also relative to the 'backend' context
COPY ai-assistant-service/pom.xml ai-assistant-service/mvnw ./service-build/
COPY ai-assistant-service/.mvn ./service-build/.mvn
# Run from the pom in the new directory
RUN mvn -B -f service-build/pom.xml -q dependency:go-offline

# Copy the main application's source code
COPY ai-assistant-service/src ./service-build/src
# Package the main application
RUN mvn -B -f service-build/pom.xml -DskipTests clean package

# STAGE 2: Final Image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built JAR from the 'build' stage
# Make sure to change 'my-microservice' here as well.
COPY --from=build /app/service-build/target/*-SNAPSHOT.jar app.jar

EXPOSE 8888
ENTRYPOINT ["java", "-jar", "/app/app.jar"]