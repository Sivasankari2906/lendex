FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy pom and source
COPY pom.xml .
COPY src ./src
COPY mvnw .
COPY .mvn .mvn

# Build with Maven wrapper (skip tests for speed)
RUN ./mvnw -B -DskipTests clean package

# Run stage
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
