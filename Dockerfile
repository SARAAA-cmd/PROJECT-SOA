# Construction
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# Exécution
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/gestion-stages.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
