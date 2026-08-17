# --- Build Stage ---
FROM amazoncorretto:25-alpine-jdk AS build
WORKDIR /workspace

COPY gradlew ./
COPY gradle gradle
COPY settings.gradle build.gradle ./

COPY steam/build.gradle steam/build.gradle
COPY catalog/build.gradle catalog/build.gradle
COPY auth/build.gradle auth/build.gradle
COPY lobby/build.gradle lobby/build.gradle
COPY app/build.gradle app/build.gradle

RUN chmod +x gradlew
# отдельным слоем качаем зависимости — кэшируется, пока build.gradle не меняются
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY steam steam
COPY catalog catalog
COPY auth auth
COPY lobby lobby
COPY app app

RUN ./gradlew --no-daemon :app:bootJar

# --- Runtime Stage ---
FROM amazoncorretto:25-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /workspace/app/build/libs/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
