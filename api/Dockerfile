# 의존성 다운로드를 소스 복사보다 위에 둔다. 소스만 고쳤을 때 이 레이어가 재사용돼서
# 재빌드가 몇 분 → 몇 초가 된다 (tasks.md T1.8 함정 ③)
FROM gradle:8.14.3-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
RUN gradle --no-daemon dependencies
COPY src ./src
RUN gradle --no-daemon bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
# 컨테이너 안 JVM도 UTC. 서버 로컬 시각이 끼어들면 월별 요약이 틀어진다 (stack.md §2.2)
ENV TZ=UTC
RUN useradd --system --uid 1001 app
USER app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
