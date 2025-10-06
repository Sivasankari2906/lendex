# Stage 1: Build Spring Boot app
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

COPY backend/pom.xml ./
COPY backend/src ./src
COPY backend/mvnw ./
COPY backend/.mvn ./.mvn

RUN ./mvnw -B -DskipTests clean package

# Stage 2: Runtime image
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
