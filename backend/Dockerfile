# Stage 1: Builder
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# gradlew가 CRLF로 체크아웃돼도 컨테이너 안에서 CR 제거 → shebang 깨짐 방지
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src

RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN adduser -D crisfin

COPY --from=builder /app/build/libs/*.jar app.jar

USER crisfin

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
