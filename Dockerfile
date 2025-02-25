FROM gradle:jdk21 AS builder
WORKDIR /app

COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle/ gradle/
RUN chmod +x gradlew

COPY src src
RUN ./gradlew clean shadowJar --no-daemon

FROM amazoncorretto:21
WORKDIR /app

COPY --from=builder /app/build/libs/app.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]