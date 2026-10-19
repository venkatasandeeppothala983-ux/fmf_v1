# Build the Spring Boot executable JAR.
FROM maven:3.9.9-eclipse-temurin-11 AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY src ./src
RUN mvn -B -DskipTests package

# Small runtime image; Render supplies PORT and terminates HTTPS at its edge.
FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /workspace/target/find-my-faculty-backend-0.1.0.jar /app/app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=55.0 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
