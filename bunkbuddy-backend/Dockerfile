# Build stage
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:17-slim
WORKDIR /app

# Install Tesseract OCR which is required by tess4j
RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-eng && \
    rm -rf /var/lib/apt/lists/*

# Copy the built jar file
COPY --from=build /app/target/bunkbuddy-backend-0.0.1-SNAPSHOT.jar app.jar

# Copy tessdata for OCR
COPY tessdata ./tessdata

# Set the port environment variable
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
