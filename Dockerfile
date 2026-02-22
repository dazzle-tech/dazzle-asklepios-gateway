FROM gradle:8.5-jdk21 AS build

WORKDIR /app

COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle

RUN gradle build --no-daemon || return 0

COPY . .

RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /application

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
