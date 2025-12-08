# 빌드 스테이지
FROM gradle:8.14.3-jdk17 AS builder
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app

COPY . .

RUN gradle clean build -x test --no-daemon

# 런타임 스테이지
FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]