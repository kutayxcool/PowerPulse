# --- Build asamasi: Maven ile jar'i derler ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Calisma asamasi: sadece JRE + derlenmis jar ---
# Not: Sensors bir web sunucusu degil (spring-boot-starter-web yok),
# disariya HTTP portu acmaz - sadece Kafka/Ignite'a ve Core'un
# /api/internal/** ucuna GIDEN baglantilar kurar. Bu yuzden EXPOSE yok.
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
