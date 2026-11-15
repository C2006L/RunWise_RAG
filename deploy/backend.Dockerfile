FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY RunWise_Backend/pom.xml ./pom.xml
RUN mvn -B dependency:go-offline
COPY RunWise_Backend/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -g 10001 app && useradd -u 10001 -g app -m app \
    && mkdir -p /app/uploads && chown -R app:app /app
WORKDIR /app
COPY --from=build /build/target/runwise-backend-1.0.0.jar /app/app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65", "-jar", "/app/app.jar"]
