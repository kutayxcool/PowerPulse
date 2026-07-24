# --- Build asamasi: Maven ile jar'i derler ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Once sadece pom.xml kopyalanir ki bagimliliklar (degismedigi surece)
# Docker'in katman cache'inden faydalansin - src degistiginde bagimliliklar
# tekrar indirilmez, build cok daha hizli olur.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Calisma asamasi: sadece JRE + derlenmis jar (kucuk/guvenli imaj) ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
