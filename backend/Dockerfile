# --- Etapa 1: build con Maven ---
# El proyecto no versiona el Maven Wrapper (se ha usado el Maven embebido de
# IntelliJ durante el desarrollo), asi que esta etapa usa una imagen con
# Maven instalado en vez de ./mvnw.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Etapa 2: runtime liviano (solo JRE, sin JDK ni Maven) ---
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --create-home appuser
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
