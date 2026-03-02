FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# 1) Gradle wrapper / 설정파일 먼저 복사 (캐시 최적화)
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle* settings.gradle* gradle.properties* ./

RUN chmod +x ./gradlew

RUN ./gradlew --no-daemon dependencies

# 2) 소스 복사
COPY src/ src/

# 3) 빌드 (에러 원인 보이게)
RUN ./gradlew clean bootJar -x test --no-daemon --stacktrace --info

# run stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]