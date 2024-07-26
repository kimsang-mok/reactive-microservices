# Stage 1: Build the application
FROM eclipse-temurin:21-jdk as builder
WORKDIR /workspace

# Copy the build artifacts to the container
COPY build/libs/*.jar app.jar

# Extract the layers from the JAR file
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 2: Create the final image
FROM eclipse-temurin:21-jre
WORKDIR /application

# Copy the extracted layers to the final image
COPY --from=builder /workspace/dependencies/ ./
COPY --from=builder /workspace/spring-boot-loader/ ./
COPY --from=builder /workspace/snapshot-dependencies/ ./
COPY --from=builder /workspace/application/ ./

# Expose the application port
EXPOSE 8080

# Set the entry point to launch the application
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
