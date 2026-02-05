# Build stage (Debian base to support arm64/amd64)
FROM --platform=$BUILDPLATFORM maven:3.9.6-eclipse-temurin-17 AS build
ARG TARGETPLATFORM
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
ENV APP_HOME=/app
WORKDIR ${APP_HOME}
COPY --from=build /workspace/target/hello-world-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
